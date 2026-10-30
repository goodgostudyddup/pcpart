# -*- coding: utf-8 -*-
"""
BuildCores 全站配件价格爬取脚本（多策略匹配版）
- 数据源: https://api.buildcores.com/trpc/parts.getPartListItems (官方前端同款接口)
- 8 个类别: CPU / CPUCooler / GPU / Motherboard / PCCase / PSU / RAM / Storage
- 取 pricing.lowestPrice (美元最低价), 按汇率换算人民币, 回写 part_* 表
  price_usd / price_cny / price_updated_at（按行 id 更新）

匹配策略（按优先级）:
  1. name 精确匹配
  2. 归一化匹配（小写+去符号，解决 "B850-A" vs "B850 A"、大小写差异）
  3. part_number 子串匹配（从本地 raw_json 解析 part_numbers，解决 GPU
     接口无 name、只能用零售商商品名的问题，如 GV-N5060WF2MAX）
  4. token 重叠匹配（要求>=4个公共词且至少1个含数字的型号词，如 5060/RX570）

使用:
    pip install requests pymysql
    py scripts/fetch_buildcores_prices.py --test            # 每类前3条试跑(只打印不写库)
    py scripts/fetch_buildcores_prices.py --test --write    # 试跑并写库
    py scripts/fetch_buildcores_prices.py --write           # 全量并写库
    py scripts/fetch_buildcores_prices.py --write --rate 7  # 全量, 美元转人民币汇率7.0
"""
import argparse
import json
import re
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime

import pymysql
import requests

DB_CONFIG = dict(host="127.0.0.1", port=3306, user="root", password="root",
                 database="pcpart", charset="utf8mb4")

TRPC_URL = "https://api.buildcores.com/trpc/parts.getPartListItems"
HEADERS = {
    "User-Agent": ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                   "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"),
    "Accept": "application/json",
    "Content-Type": "application/json",
    "Origin": "https://www.buildcores.com",
    "Referer": "https://www.buildcores.com/",
}

CATEGORY_TABLE = {
    "CPU": "part_cpu",
    "CPUCooler": "part_cpu_cooler",
    "GPU": "part_gpu",
    "Motherboard": "part_motherboard",
    "PCCase": "part_pc_case",
    "PSU": "part_psu",
    "RAM": "part_ram",
    "Storage": "part_storage",
}

PAGE_SIZE = 100
MAX_WORKERS = 4
FETCH_RETRIES = 4
FETCH_BACKOFF = 3.0


def new_session():
    s = requests.Session()
    s.headers.update(HEADERS)
    adapter = requests.adapters.HTTPAdapter(pool_connections=MAX_WORKERS + 2,
                                            pool_maxsize=(MAX_WORKERS + 2) * 2,
                                            max_retries=0)
    s.mount("https://", adapter)
    s.mount("http://", adapter)
    s.trust_env = False
    return s


def normalize(s):
    return re.sub(r"\s+", " ", re.sub(r"[^a-z0-9]+", " ", str(s).lower())).strip()


def model_tokens(norm_name):
    return {t for t in norm_name.split() if len(t) >= 3 and any(c.isdigit() for c in t)}


def fetch_category(category, limit=0, session=None):
    """分页拉取某类别全部商品，返回 [(api_name, usd), ...]。"""
    session = session or new_session()
    items = []
    seen = set()
    skip = 0
    while True:
        payload = {"partCategory": category, "limit": PAGE_SIZE, "skip": skip,
                   "filters": [], "inStockOnly": False, "showInteractiveFirst": True,
                   "region": "US", "includeTest": False, "logAnalytics": True}
        params = {"input": json.dumps(payload, separators=(",", ":"))}
        for attempt in range(FETCH_RETRIES):
            try:
                r = session.get(TRPC_URL, params=params, timeout=30)
                r.raise_for_status()
                break
            except requests.RequestException as e:
                if attempt == FETCH_RETRIES - 1:
                    raise RuntimeError(f"分页skip={skip}失败(重试{FETCH_RETRIES}次): {e}")
                time.sleep(FETCH_BACKOFF * (attempt + 1))
        data = r.json()
        try:
            records = data["result"]["data"]["data"]
        except (KeyError, IndexError, TypeError):
            raise RuntimeError(f"接口返回结构变化: {r.text[:300]}")
        if not records:
            break
        for rec in records:
            name = (rec.get("name") or "").strip()
            if not name:
                offers = ((rec.get("pricing") or {}).get("offers")) or []
                if offers:
                    name = (offers[0].get("retailerProductName") or "").strip()
            if not name:
                continue
            pricing = rec.get("pricing") or {}
            try:
                lowest = float(pricing.get("lowestPrice"))
            except (TypeError, ValueError):
                continue
            if name not in seen:
                seen.add(name)
                items.append((name, lowest))
        skip += len(records)
        if len(records) < PAGE_SIZE:
            break
        if limit and len(items) >= limit:
            break
        time.sleep(0.3)
    return items[:limit] if limit else items


def load_local_index(cur, table):
    """返回 (exact, norm_map, pn_map, token_rows)：本地行的多索引。"""
    cur.execute(f"SELECT `id`, `name`, `raw_json` FROM `{table}`")
    rows = cur.fetchall()
    exact, norm_map, pn_map, token_rows = {}, {}, {}, []
    for rid, name, raw in rows:
        name = (name or "").strip()
        if not name:
            continue
        exact.setdefault(name, []).append(rid)
        nn = normalize(name)
        norm_map.setdefault(nn, []).append(rid)
        token_rows.append((rid, set(nn.split()), model_tokens(nn)))
        try:
            pns = ((json.loads(raw) if isinstance(raw, str) else raw or {}).get("metadata") or {}).get("part_numbers") or []
        except (ValueError, TypeError, AttributeError):
            pns = []
        for pn in pns:
            key = re.sub(r"[^a-z0-9]", "", str(pn).lower())
            if len(key) >= 6:
                pn_map.setdefault(key, []).append(rid)
    return exact, norm_map, pn_map, token_rows


def match_price(api_name, usd, exact, norm_map, pn_map, token_rows):
    """返回 (rid_list, 策略名)。"""
    if api_name in exact:
        return exact[api_name], "exact"
    nn = normalize(api_name)
    if nn in norm_map:
        return norm_map[nn], "norm"
    flat = nn.replace(" ", "")
    pn_hits = []
    for pn, rids in pn_map.items():
        if pn in flat:
            pn_hits.extend(rids)
    if pn_hits:
        return sorted(set(pn_hits)), "pn"
    api_toks = set(nn.split())
    api_models = model_tokens(nn)
    best, best_score = [], (-1, -1)
    for rid, toks, models in token_rows:
        common = api_toks & toks
        model_hit = api_models & models
        if len(common) >= 4 and model_hit:
            score = (len(model_hit), len(common))
            if score > best_score:
                best, best_score = [rid], score
            elif score == best_score:
                best.append(rid)
    if best:
        return best, "overlap"
    return [], "miss"


def ensure_price_columns(cur, table):
    cur.execute("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                "WHERE TABLE_SCHEMA='pcpart' AND TABLE_NAME=%s", (table,))
    existing = {row[0] for row in cur.fetchall()}
    defs = {
        "price_usd": "DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'BuildCores lowest USD price'",
        "price_cny": "DECIMAL(12,2) NULL DEFAULT NULL COMMENT 'CNY converted price'",
        "price_updated_at": "DATETIME NULL DEFAULT NULL COMMENT 'price updated at'",
    }
    for col, ddl in defs.items():
        if col not in existing:
            cur.execute(f"ALTER TABLE `{table}` ADD COLUMN `{col}` {ddl}")
            print(f"  [列] {table}.{col} 已添加")


def process_category(category, table, limit, rate, write):
    """单个类别：抓取->匹配->[写库]，返回结果dict。每个线程独立DB连接。"""
    session = new_session()
    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()
    try:
        api_items = fetch_category(category, limit, session)
        exact, norm_map, pn_map, token_rows = load_local_index(cur, table)
        stats = {"exact": 0, "norm": 0, "pn": 0, "overlap": 0, "miss": 0}
        id_price = {}
        miss_ex = []
        for api_name, usd in api_items:
            rids, how = match_price(api_name, usd, exact, norm_map, pn_map, token_rows)
            stats[how] += 1
            if how == "miss":
                if len(miss_ex) < 3:
                    miss_ex.append(api_name)
                continue
            for rid in rids:
                if rid not in id_price or usd < id_price[rid]:
                    id_price[rid] = usd
        wrote = 0
        if write and id_price:
            ensure_price_columns(cur, table)
            now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            data = [(usd, round(usd * rate, 2), now, rid) for rid, usd in id_price.items()]
            cur.executemany(f"UPDATE `{table}` SET `price_usd`=%s, `price_cny`=%s, "
                            f"`price_updated_at`=%s WHERE `id`=%s", data)
            conn.commit()
            wrote = len(data)
        return {"category": category, "table": table, "api": len(api_items),
                "rows": len(id_price), "wrote": wrote, "stats": stats,
                "miss_ex": miss_ex, "sample": api_items[:5]}
    finally:
        cur.close()
        conn.close()


def main():
    ap = argparse.ArgumentParser(description="BuildCores配件价格爬取(多策略匹配,8类并行)")
    ap.add_argument("--test", action="store_true", help="每类只取前3条")
    ap.add_argument("--write", action="store_true", help="写回数据库(默认只打印)")
    ap.add_argument("--rate", type=float, default=7.0, help="美元转人民币汇率, 默认7.0")
    ap.add_argument("--only", default="", help="只抓指定类别, 逗号分隔, 如 CPU,GPU")
    ap.add_argument("--limit", type=int, default=0, help="每类最多抓取条数, 0=全量")
    ap.add_argument("--workers", type=int, default=MAX_WORKERS, help="并行线程数, 默认4(8并发易被限流)")
    args = ap.parse_args()

    only = {x.strip() for x in args.only.split(",") if x.strip()} if args.only else set()
    jobs = [(c, t) for c, t in CATEGORY_TABLE.items() if not only or c in only]
    limit_of = 3 if args.test else args.limit

    grand_api = grand_rows = grand_miss = 0
    with ThreadPoolExecutor(max_workers=max(1, args.workers)) as pool:
        futures = {pool.submit(process_category, c, t, limit_of, args.rate, args.write): (c, t)
                   for c, t in jobs}
        for fut in as_completed(futures):
            category, table = futures[fut]
            try:
                res = fut.result()
            except Exception as e:
                print(f"\n== {category} -> {table} ==\n  抓取失败: {e}")
                continue
            print(f"\n== {res['category']} -> {res['table']} ==")
            print(f"  接口 {res['api']} 条, 命中 {res['rows']} 行")
            for name in res["miss_ex"]:
                print(f"    未匹配: {name[:110]}")
            s = res["stats"]
            print(f"  策略: exact={s['exact']} norm={s['norm']} "
                  f"pn={s['pn']} overlap={s['overlap']} miss={s['miss']}")
            for api_name, usd in res["sample"]:
                print(f"    {api_name[:90]} -> ${usd:.2f} / 约{usd * args.rate:.2f}元")
            if args.write and res["wrote"]:
                print(f"  已写库 {res['wrote']} 行")
            grand_api += res["api"]
            grand_rows += res["rows"]
            grand_miss += s["miss"]

    print(f"\n接口共 {grand_api} 条, 命中 {grand_rows} 行, 未匹配 {grand_miss} 条"
          + ("" if args.write else " (未写库)"))


if __name__ == "__main__":
    main()
