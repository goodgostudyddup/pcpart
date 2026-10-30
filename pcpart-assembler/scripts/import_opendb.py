# -*- coding: utf-8 -*-
"""
BuildCore open-db JSON 导入脚本
- 读取 db/buildcores-open-db/open-db 下 8 类 JSON
- 提取关键列 + 原文 raw_json, 写入 part_* 表 (opendb_id 唯一, 可重复执行)
- 用法:
    py scripts/import_opendb.py --limit 100      # 小批量验证: 每类前100条
    py scripts/import_opendb.py --limit 0        # 全量导入 (25196条, 较慢)
    py scripts/import_opendb.py --limit 100 --truncate  # 先清空再导
"""
import argparse
import glob
import json
import os
import sys

import pymysql

DB_CONFIG = dict(host="127.0.0.1", port=3306, user="root", password="root",
                 database="pcpart", charset="utf8mb4")
BASE_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                        "..", "db", "buildcores-open-db", "open-db")
BASE_DIR = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                         "..", "..", "db", "buildcores-open-db", "open-db"))

CATEGORIES = ["CPU", "CPUCooler", "GPU", "Motherboard", "PCCase", "PSU", "RAM", "Storage"]


def g(d, *path):
    cur = d
    for p in path:
        if not isinstance(cur, dict) or p not in cur:
            return None
        cur = cur[p]
    return cur


def s(v, maxlen=None):
    if v is None:
        return None
    if isinstance(v, bool):
        return "1" if v else "0"
    t = str(v)
    if maxlen and len(t) > maxlen:
        t = t[:maxlen]
    return t


def num(v):
    if v is None or isinstance(v, bool):
        return None
    if isinstance(v, (int, float)):
        return v
    return None


def bint(v):
    if v is None:
        return None
    if isinstance(v, bool):
        return 1 if v else 0
    if isinstance(v, (int, float)):
        return int(v)
    return None


def join_list(v, maxlen=None):
    if not isinstance(v, list) or not v:
        return None
    t = ", ".join(str(x) for x in v)
    if maxlen and len(t) > maxlen:
        t = t[:maxlen]
    return t


def meta(d):
    m = d.get("metadata") or {}
    return {
        "name": s(m.get("name"), 255),
        "manufacturer": s(m.get("manufacturer"), 100),
        "series": s(m.get("series"), 100),
        "variant": s(m.get("variant"), 100),
        "release_year": num(m.get("releaseYear")),
    }


def map_cpu(d):
    m = meta(d)
    ig = s(g(d, "specifications", "integratedGraphics", "model"))
    if ig in (None, "", "None"):
        ig = None
    return {
        "opendb_id": d.get("opendb_id"), "name": m["name"], "manufacturer": m["manufacturer"],
        "series": s(g(d, "series"), 100), "variant": m["variant"],
        "socket": s(d.get("socket"), 50), "microarchitecture": s(d.get("microarchitecture"), 100),
        "core_family": s(d.get("coreFamily"), 100),
        "cores_total": num(g(d, "cores", "total")), "cores_performance": num(g(d, "cores", "performance")),
        "cores_efficiency": num(g(d, "cores", "efficiency")), "threads": num(g(d, "cores", "threads")),
        "base_clock": num(g(d, "clocks", "performance", "base")),
        "boost_clock": num(g(d, "clocks", "performance", "boost")),
        "l2_cache": num(g(d, "cache", "l2")), "l3_cache": num(g(d, "cache", "l3")),
        "tdp": num(g(d, "specifications", "tdp")), "integrated_graphics": ig,
        "memory_max": num(g(d, "specifications", "memory", "maxSupport")),
        "release_year": m["release_year"], "raw_json": json.dumps(d, ensure_ascii=False),
    }


def map_cooler(d):
    m = meta(d)
    return {
        "opendb_id": d.get("opendb_id"), "name": m["name"], "manufacturer": m["manufacturer"],
        "series": m["series"], "variant": m["variant"],
        "water_cooled": bint(d.get("water_cooled")), "radiator_size": num(d.get("radiator_size")),
        "fanless": bint(d.get("fanless")), "fan_size": num(d.get("fan_size")),
        "fan_quantity": num(d.get("fan_quantity")),
        "min_fan_rpm": num(d.get("min_fan_rpm")), "max_fan_rpm": num(d.get("max_fan_rpm")),
        "min_noise": num(d.get("min_noise_level")), "max_noise": num(d.get("max_noise_level")),
        "height": num(d.get("height")), "cpu_sockets": join_list(d.get("cpu_sockets"), 500),
        "release_year": m["release_year"], "raw_json": json.dumps(d, ensure_ascii=False),
    }


def map_gpu(d):
    m = meta(d)
    return {
        "opendb_id": d.get("opendb_id"), "name": m["name"], "manufacturer": m["manufacturer"],
        "series": m["series"], "variant": m["variant"],
        "chipset_manufacturer": s(d.get("chipset_manufacturer"), 50), "chipset": s(d.get("chipset"), 100),
        "core_base_clock": num(d.get("core_base_clock")), "core_boost_clock": num(d.get("core_boost_clock")),
        "memory_gb": num(d.get("memory")), "memory_type": s(d.get("memory_type"), 50),
        "effective_memory_clock": num(d.get("effective_memory_clock")), "tdp": num(d.get("tdp")),
        "gpu_interface": s(d.get("interface"), 50), "length_mm": num(d.get("length")),
        "slot_width": num(d.get("total_slot_width")), "cooling": s(d.get("cooling"), 100),
        "frame_sync": s(d.get("frame_sync"), 50),
        "release_year": m["release_year"], "raw_json": json.dumps(d, ensure_ascii=False),
    }


def map_motherboard(d):
    m = meta(d)
    return {
        "opendb_id": d.get("opendb_id"), "name": m["name"], "manufacturer": m["manufacturer"],
        "series": m["series"], "variant": m["variant"],
        "socket": s(d.get("socket"), 50), "chipset": s(d.get("chipset"), 100),
        "form_factor": s(d.get("form_factor"), 50),
        "memory_max": num(g(d, "memory", "max")), "memory_type": s(g(d, "memory", "ram_type"), 50),
        "memory_slots": num(g(d, "memory", "slots")),
        "wireless": s(d.get("wireless_networking"), 100),
        "ecc_support": bint(d.get("ecc_support")), "raid_support": bint(d.get("raid_support")),
        "color": join_list(d.get("color"), 255),
        "release_year": m["release_year"], "raw_json": json.dumps(d, ensure_ascii=False),
    }


def map_case(d):
    m = meta(d)
    return {
        "opendb_id": d.get("opendb_id"), "name": m["name"], "manufacturer": m["manufacturer"],
        "series": m["series"], "variant": m["variant"],
        "form_factor": s(d.get("form_factor"), 50), "color": join_list(d.get("color"), 255),
        "side_panel": s(d.get("side_panel"), 100),
        "transparent_panel": bint(d.get("has_transparent_side_panel")),
        "power_supply": s(d.get("power_supply"), 100),
        "max_gpu_length": num(d.get("max_video_card_length")),
        "max_cooler_height": num(d.get("max_cpu_cooler_height")),
        "bays_35": num(d.get("internal_3_5_bays")), "bays_25": num(d.get("internal_2_5_bays")),
        "expansion_slots": num(d.get("expansion_slots")),
        "volume_l": num(d.get("volume")), "weight_kg": num(d.get("weight")),
        "release_year": m["release_year"], "raw_json": json.dumps(d, ensure_ascii=False),
    }


def map_psu(d):
    m = meta(d)
    return {
        "opendb_id": d.get("opendb_id"), "name": m["name"], "manufacturer": m["manufacturer"],
        "series": m["series"], "variant": m["variant"],
        "wattage": num(d.get("wattage")), "form_factor": s(d.get("form_factor"), 50),
        "efficiency": s(d.get("efficiency_rating"), 50), "modular": s(d.get("modular"), 50),
        "length_mm": num(d.get("length")), "fanless": bint(d.get("fanless")),
        "release_year": m["release_year"], "raw_json": json.dumps(d, ensure_ascii=False),
    }


def map_ram(d):
    m = meta(d)
    return {
        "opendb_id": d.get("opendb_id"), "name": m["name"], "manufacturer": m["manufacturer"],
        "series": m["series"], "variant": m["variant"],
        "ram_type": s(d.get("ram_type"), 20), "speed_mt": num(d.get("speed")),
        "capacity_gb": num(d.get("capacity")),
        "modules_qty": num(g(d, "modules", "quantity")), "module_gb": num(g(d, "modules", "capacity_gb")),
        "form_factor": s(d.get("form_factor"), 50), "cas_latency": num(d.get("cas_latency")),
        "voltage": num(d.get("voltage")), "color": join_list(d.get("color"), 255),
        "release_year": m["release_year"], "raw_json": json.dumps(d, ensure_ascii=False),
    }


def map_storage(d):
    m = meta(d)
    return {
        "opendb_id": d.get("opendb_id"), "name": m["name"], "manufacturer": m["manufacturer"],
        "series": m["series"], "variant": m["variant"],
        "storage_type": s(d.get("storage_type"), 20), "capacity_gb": num(d.get("capacity")),
        "form_factor": s(d.get("form_factor"), 50), "hard_interface": s(d.get("interface"), 100),
        "nvme": bint(d.get("nvme")), "cache_mb": num(d.get("cache")),
        "release_year": m["release_year"], "raw_json": json.dumps(d, ensure_ascii=False),
    }


TABLE_MAP = {
    "CPU": ("part_cpu", map_cpu),
    "CPUCooler": ("part_cpu_cooler", map_cooler),
    "GPU": ("part_gpu", map_gpu),
    "Motherboard": ("part_motherboard", map_motherboard),
    "PCCase": ("part_pc_case", map_case),
    "PSU": ("part_psu", map_psu),
    "RAM": ("part_ram", map_ram),
    "Storage": ("part_storage", map_storage),
}


def import_category(cur, conn, category, table, mapper, limit, batch=200):
    files = sorted(glob.glob(os.path.join(BASE_DIR, category, "*.json")))
    if limit and limit > 0:
        files = files[:limit]
    rows = []
    skipped = 0
    for f in files:
        try:
            d = json.load(open(f, encoding="utf-8"))
        except Exception as e:
            skipped += 1
            print(f"    跳过坏文件 {os.path.basename(f)}: {e}")
            continue
        try:
            row = mapper(d)
        except Exception as e:
            skipped += 1
            print(f"    跳过解析失败 {os.path.basename(f)}: {e}")
            continue
        if not row.get("opendb_id") or not row.get("name"):
            skipped += 1
            continue
        rows.append(row)
    if not rows:
        print(f"  {category}: 无有效数据")
        return 0
    cols = list(rows[0].keys())
    col_sql = ", ".join(f"`{c}`" for c in cols)
    ph = ", ".join(["%s"] * len(cols))
    upd = ", ".join(f"`{c}`=VALUES(`{c}`)" for c in cols if c != "opendb_id")
    sql = f"INSERT INTO `{table}` ({col_sql}) VALUES ({ph}) ON DUPLICATE KEY UPDATE {upd}"
    total = 0
    for i in range(0, len(rows), batch):
        chunk = rows[i:i + batch]
        cur.executemany(sql, [[r[c] for c in cols] for r in chunk])
        conn.commit()
        total += len(chunk)
    print(f"  {category} -> {table}: 文件{len(files)} 有效{len(rows)} 跳过{skipped} 入库{total}")
    return total


def main():
    ap = argparse.ArgumentParser(description="open-db JSON导入")
    ap.add_argument("--limit", type=int, default=100, help="每类导入条数, 0=全量")
    ap.add_argument("--truncate", action="store_true", help="导入前清空8张表")
    ap.add_argument("--only", default="", help="只导指定类别, 逗号分隔, 如 GPU,RAM")
    args = ap.parse_args()

    if not os.path.isdir(BASE_DIR):
        print(f"目录不存在: {BASE_DIR}")
        sys.exit(1)

    only = {x.strip() for x in args.only.split(",") if x.strip()} if args.only else set()
    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()
    if args.truncate:
        for cat, (table, _) in TABLE_MAP.items():
            if only and cat not in only:
                continue
            cur.execute(f"TRUNCATE TABLE `{table}`")
        conn.commit()
        print("已清空目标表")

    grand = 0
    for cat, (table, mapper) in TABLE_MAP.items():
        if only and cat not in only:
            continue
        print(f"== {cat} ==")
        grand += import_category(cur, conn, cat, table, mapper, args.limit)
    print(f"\n共入库 {grand} 条")
    for _, (t, _) in TABLE_MAP.items():
        cur.execute(f"SELECT COUNT(*) FROM `{t}`")
        print(f"  {t}: {cur.fetchone()[0]}")
    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
