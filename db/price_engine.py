# -*- coding: utf-8 -*-
"""给配件库批量生成合理价格。

原理
----
1. 用库里已有的真实价格做锚样本，对每个配件类别拟合「对数价格 ~ 规格特征」的
   岭回归（log-linear hedonic model），规格含核心数/显存/容量/功率/尺寸等数值
   特征和厂商/芯片/接口/效率等级等类别特征。
2. 5 折交叉验证估误差，按类别挑最优正则强度。
3. 缺失价格的配件用模型预测，再按细分档（如 SSD/HDD + 规格）做分位数校准，
   让每一档的价格分布与真实锚样本一致。
4. 价格落地成电商习惯的尾数（.00 / .99），人民币按固定 7.0 汇率换算，
   并把 price_updated_at 打上生成标记，方便随时区分真实价与生成价。

用法
----
    python price_engine.py --dry-run          # 只验证、只打印样本，不写库
    python price_engine.py --apply            # 生成并写库（只填 price_cny 为 NULL 的行）
    python price_engine.py --apply --all-rows # 连已有价一起重算（慎用）
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys

import numpy as np
import pandas as pd

MYSQL = r"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe"
DB = "pcpart"
USER = "root"
PASSWORD = "root"
RATE = 7.0                                    # 真实数据里 price_cny / price_usd 恒为 7.000
GEN_STAMP = "2026-10-19 00:00:00"             # 生成价标记，用于区分真实抓取价
SEED = 20261019

# 每个类别的建模配置：table=表名, feats=特征, seg=校准分档, corridor=裁剪区间
CATS: dict[str, dict] = {
    "cpu": {
        "table": "part_cpu",
        "feats": [("log", "cores_total"), ("log", "threads"), ("log", "boost_clock"),
                  ("log", "l3_cache"), ("log", "tdp"), ("lin", "release_year"),
                  ("cat", "manufacturer", 8), ("cat", "socket", 20), ("cat", "core_family", 24)],
        "seg": ["manufacturer"], "corridor": (0.55, 1.35), "zero_ok": [],
    },
    "gpu": {
        "table": "part_gpu",
        "feats": [("log", "memory_gb"), ("log", "tdp"), ("log", "core_boost_clock"),
                  ("log", "length_mm"), ("lin", "release_year"),
                  ("cat", "chipset_manufacturer", 8), ("cat", "chipset", 60),
                  ("cat", "memory_type", 10), ("cat", "cooling", 10), ("cat", "frame_sync", 8)],
        "seg": ["memory_type"], "corridor": (0.55, 1.35), "zero_ok": [],
    },
    "motherboard": {
        "table": "part_motherboard",
        "feats": [("log", "memory_slots"), ("log", "memory_max"), ("lin", "release_year"),
                  ("cat", "socket", 20), ("cat", "chipset", 40), ("cat", "form_factor", 8),
                  ("cat", "memory_type", 8), ("cat", "wireless", 8), ("cat", "manufacturer", 12)],
        "seg": ["form_factor"], "corridor": (0.6, 1.3), "zero_ok": [],
    },
    "ram": {
        "table": "part_ram",
        "feats": [("log", "capacity_gb"), ("log", "speed_mt"), ("log", "cas_latency"),
                  ("log", "modules_qty"), ("lin", "release_year"),
                  ("cat", "ram_type", 10), ("cat", "form_factor", 8),
                  ("cat", "manufacturer", 12), ("cat", "series", 24)],
        "seg": ["ram_type"], "corridor": (0.6, 1.3), "zero_ok": [],
    },
    "storage": {
        "table": "part_storage",
        "feats": [("log", "capacity_gb"), ("log", "cache_mb"), ("lin", "release_year"),
                  ("cat", "storage_type", 8), ("cat", "form_factor", 10),
                  ("cat", "hard_interface", 14), ("cat", "nvme", 4), ("cat", "manufacturer", 12)],
        "seg": ["storage_type", "form_factor"], "corridor": (0.55, 1.35), "zero_ok": [],
    },
    "psu": {
        "table": "part_psu",
        "feats": [("log", "wattage"), ("log", "length_mm"), ("lin", "release_year"),
                  ("cat", "efficiency", 10), ("cat", "modular", 8),
                  ("cat", "form_factor", 8), ("cat", "manufacturer", 12)],
        "seg": ["efficiency"], "corridor": (0.6, 1.3), "zero_ok": [],
    },
    "cooler": {
        "table": "part_cpu_cooler",
        "feats": [("log", "radiator_size"), ("log", "fan_quantity"), ("log", "height"),
                  ("log", "fan_size"), ("log", "max_noise"), ("lin", "release_year"),
                  ("cat", "water_cooled", 4), ("cat", "fanless", 4),
                  ("cat", "manufacturer", 12), ("cat", "cpu_sockets", 12)],
        "seg": ["water_cooled"], "corridor": (0.55, 1.35),
        "zero_ok": ["radiator_size"],
    },
    "pc_case": {
        "table": "part_pc_case",
        "feats": [("log", "volume_l"), ("log", "weight_kg"), ("log", "max_gpu_length"),
                  ("log", "max_cooler_height"), ("log", "expansion_slots"), ("lin", "release_year"),
                  ("cat", "form_factor", 10), ("cat", "side_panel", 10),
                  ("cat", "power_supply", 8), ("cat", "manufacturer", 12)],
        "seg": ["form_factor"], "corridor": (0.6, 1.3), "zero_ok": [],
    },
}


def query(sql: str) -> pd.DataFrame:
    out = subprocess.run(
        [MYSQL, f"-u{USER}", f"-p{PASSWORD}", "--default-character-set=utf8mb4", "-B", DB, "-e", sql],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    if out.returncode != 0:
        raise RuntimeError(out.stderr.strip())
    from io import StringIO
    return pd.read_csv(StringIO(out.stdout), sep="\t", dtype=str, keep_default_na=False, na_values=["NULL"])


def load(table: str) -> pd.DataFrame:
    return query(f"SELECT * FROM {table} ORDER BY id")


SEP = "||"          # 特征列名前缀分隔符，避免 chipset / chipset_manufacturer 这类前缀互相串味


def build_design(df: pd.DataFrame, feats, top_n_default: int = 10, zero_ok=()):
    """返回 (矩阵, 列名, 数值列中位数/均值/标准差)。"""
    cols, blocks, num_meta = [], [], []
    stats = {}
    for feat in feats:
        kind, name = feat[0], feat[1]
        if kind == "cat":
            top = feat[2] if len(feat) > 2 else top_n_default
            s = df[name].astype("string").fillna("__NA__")
            keep = s.value_counts().head(top).index.tolist()
            s = s.where(s.isin(keep), "__OTHER__")
            dummies = pd.get_dummies(s, dtype=float)
            dummies.columns = [f"{name}{SEP}{v}" for v in dummies.columns]
            blocks.append(dummies.to_numpy())
            cols.extend(dummies.columns.tolist())
        else:
            v = pd.to_numeric(df[name], errors="coerce")
            if name not in zero_ok:            # 0 在规格里通常代表“采集缺失”，不是真实零值
                v = v.where(v > 0)
            med = float(v.median()) if v.notna().any() else 0.0
            v = v.fillna(med).clip(lower=0)
            arr = np.log1p(v.to_numpy(dtype=float)) if kind == "log" else v.to_numpy(dtype=float)
            stats[name] = (med, float(arr.mean()), float(arr.std() or 1.0))
            blocks.append(arr.reshape(-1, 1))
            cols.append(name)
    X = np.hstack(blocks) if blocks else np.zeros((len(df), 0))
    for idx, name in enumerate(cols):
        if name in stats:
            med, mu, sd = stats[name]
            X[:, idx] = (X[:, idx] - mu) / sd
    return X, cols, stats


def design_new(df: pd.DataFrame, feats, stats, colnames, zero_ok=()):
    blocks = []
    for feat in feats:
        kind, name = feat[0], feat[1]
        if kind == "cat":
            train_cols = [c for c in colnames if c.startswith(name + SEP)]
            known = {c.split(SEP, 1)[1] for c in train_cols}
            s = df[name].astype("string").fillna("__NA__")
            s = s.where(s.isin(known), "__OTHER__")
            dummies = pd.get_dummies(s, dtype=float)
            dummies.columns = [f"{name}{SEP}{v}" for v in dummies.columns]
            dummies = dummies.reindex(columns=train_cols, fill_value=0.0)
            blocks.append(dummies.to_numpy())
        else:
            v = pd.to_numeric(df[name], errors="coerce")
            if name not in zero_ok:
                v = v.where(v > 0)
            med, mu, sd = stats[name]
            v = v.fillna(med).clip(lower=0)
            arr = np.log1p(v.to_numpy(dtype=float)) if kind == "log" else v.to_numpy(dtype=float)
            blocks.append(((arr - mu) / sd).reshape(-1, 1))
    return np.hstack(blocks) if blocks else np.zeros((len(df), 0))


def fit_ridge(X: np.ndarray, y: np.ndarray, lam: float) -> np.ndarray:
    Xb = np.hstack([np.ones((X.shape[0], 1)), X])
    pen = np.eye(Xb.shape[1])
    pen[0, 0] = 0.0
    return np.linalg.solve(Xb.T @ Xb + lam * pen, Xb.T @ y)


def predict(X: np.ndarray, beta: np.ndarray) -> np.ndarray:
    Xb = np.hstack([np.ones((X.shape[0], 1)), X])
    return Xb @ beta


def cv_score(X, y, lam, folds=5):
    rng = np.random.default_rng(SEED)
    idx = rng.permutation(len(y))
    parts = np.array_split(idx, folds)
    ape = []
    for k in range(folds):
        te = parts[k]
        tr = np.concatenate([parts[j] for j in range(folds) if j != k])
        beta = fit_ridge(X[tr], y[tr], lam)
        pred = predict(X[te], beta)
        ape.append(np.abs(np.expm1(pred) - np.expm1(y[te])) / np.expm1(y[te]))
    ape = np.concatenate(ape)
    return float(np.median(ape)), float(ape.mean()), float((ape <= 0.25).mean()), float((ape <= 0.50).mean())


def snap_usd(value_cny: float) -> float:
    v = value_cny / RATE
    if v < 10:
        step = 1.0
    elif v < 50:
        step = 5.0
    elif v < 200:
        step = 10.0
    elif v < 1000:
        step = 25.0
    else:
        step = 50.0
    usd = round(v / step) * step
    if usd >= 20 and int(round(usd / step)) % 2 == 1:   # 一半尾数 .00，一半 .99，贴近真实数据
        usd -= 0.01
    return round(usd, 2)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true", help="真正写库")
    ap.add_argument("--dry-run", action="store_true", help="只验证")
    ap.add_argument("--all-rows", action="store_true", help="连已有价的行一起重算")
    ap.add_argument("--sql-out", default=None, help="把 UPDATE 语句写到文件")
    args = ap.parse_args()

    report = {}
    statements = []
    rng = np.random.default_rng(SEED)

    for cat, cfg in CATS.items():
        df = load(cfg["table"])
        price = pd.to_numeric(df["price_cny"], errors="coerce")
        is_real = price.notna() & (price > 0)
        if args.all_rows:
            is_priced = pd.Series(False, index=df.index)
        else:
            is_priced = is_real
        target = df[~is_priced].copy()
        price_fit = price[is_real].to_numpy(dtype=float)
        train = df[is_real].copy()

        X_tr, colnames, stats = build_design(train, cfg["feats"], zero_ok=cfg.get("zero_ok", ()))
        y_tr = np.log1p(price_fit)

        best = None
        for lam in (0.3, 1.0, 3.0, 10.0, 30.0):
            m = cv_score(X_tr, y_tr, lam)
            if best is None or m[0] < best[0][0]:
                best = (m, lam)
        metrics, lam = best
        beta = fit_ridge(X_tr, y_tr, lam)

        # 预测缺失行
        X_new = design_new(target, cfg["feats"], stats, colnames, zero_ok=cfg.get("zero_ok", ()))
        pred_cny = np.expm1(predict(X_new, beta))

        # 分档校准：让同档预测中位数贴近真实中位数
        key = "__seg__"
        seg_tr = train.copy()
        seg_tr["price_cny"] = pd.to_numeric(seg_tr["price_cny"], errors="coerce")
        seg_tr["__pred__"] = np.expm1(predict(X_tr, beta))
        seg_tr[key] = seg_tr[cfg["seg"]].astype("string").fillna("__NA__").agg("|".join, axis=1)
        tgt = target.copy()
        tgt["__pred__"] = pred_cny
        tgt[key] = tgt[cfg["seg"]].astype("string").fillna("__NA__").agg("|".join, axis=1)
        ratios, bands = {}, {}
        for seg, grp in seg_tr.groupby(key):
            if len(grp) >= 8:
                r = float(grp["price_cny"].median()) / float(grp["__pred__"].median())
                ratios[seg] = min(max(r, 0.6), 1.6)
        tgt["__pred2__"] = tgt["__pred__"] * tgt[key].map(lambda s: ratios.get(s, 1.0))

        # 品牌先验：只在规格缺失较多的行上生效，避免把规格齐全的高端件往品牌中位价拉
        brand_med = {}
        if "manufacturer" in train.columns:
            grp = seg_tr.groupby("manufacturer")["price_cny"].agg(["median", "count"])
            brand_med = {k: float(r["median"]) for k, r in grp.iterrows() if r["count"] >= 10}
        num_cols = [f[1] for f in cfg["feats"] if f[0] != "cat"]
        missing = tgt[num_cols].apply(lambda c: pd.to_numeric(c, errors="coerce") <= 0).sum(axis=1) / max(len(num_cols), 1)

        def blend(row, miss):
            base = row["__pred2__"]
            med = brand_med.get(row.get("manufacturer"))
            w = 0.35 * float(miss)
            if med and med > 0 and base > 0 and w > 0:
                base = float(np.exp((1 - w) * np.log(base) + w * np.log(med)))
            return base

        tgt["__adj__"] = [blend(row, m) for (_, row), m in zip(tgt.iterrows(), missing)]

        # 分档上限：预测价高于该档真实 p97 时做对数软压缩（不硬裁，避免批量撞限价）；
        # 低端不用分档中位数往上抬，只保留一个地板价，免得便宜的入门件被抬到没有低价位可选。
        bands = {}
        for seg, grp in seg_tr.groupby(key):
            if len(grp) >= 15:
                bands[seg] = float(np.percentile(grp["price_cny"].to_numpy(float), 97))
        cat_hi97 = float(np.percentile(price_fit, 97))
        hi_arr = np.array([max(bands.get(s, cat_hi97), 1.0) for s in tgt[key]])
        vals = tgt["__adj__"].to_numpy(float)
        soft = np.where(vals > hi_arr, hi_arr * np.power(vals / hi_arr, 0.30), vals)

        lo = max(price_fit.min() * cfg["corridor"][0], 20.0)
        hi = price_fit.max() * cfg["corridor"][1]
        tgt["__cny__"] = np.clip(soft, lo, hi).round(2)

        for _, row in tgt.iterrows():
            usd = snap_usd(float(row["__cny__"]))
            cny = round(usd * RATE, 2)
            if args.all_rows:
                statements.append(
                    f"UPDATE {cfg['table']} SET price_usd={usd:.2f}, price_cny={cny:.2f}, price_updated_at='{GEN_STAMP}' WHERE id={int(row['id'])};")
            else:
                statements.append(
                    f"UPDATE {cfg['table']} SET price_usd={usd:.2f}, price_cny={cny:.2f}, price_updated_at='{GEN_STAMP}' WHERE id={int(row['id'])} AND price_cny IS NULL;")

        sample = tgt.assign(usd=[snap_usd(float(v)) for v in tgt["__cny__"]]).head(6)
        report[cat] = {
            "table": cfg["table"], "rows": int(len(df)), "real": int(is_real.sum()),
            "fill": int(len(target)), "lambda": lam,
            "cv_median_ape": round(metrics[0], 4), "cv_mean_ape": round(metrics[1], 4),
            "cv_within_25": round(metrics[2], 4), "cv_within_50": round(metrics[3], 4),
            "sample": [
                {"name": str(r["name"])[:46], "usd": round(float(r["usd"]), 2),
                 "cny": round(float(r["usd"]) * RATE, 2)} for _, r in sample.iterrows()
            ],
        }
        print(f"[{cat:<12}] n={len(df):<5} real={int(is_real.sum()):<5} fill={len(target):<5} "
              f"lam={lam:<5} medianAPE={metrics[0]:.1%} within25={metrics[2]:.1%} within50={metrics[3]:.1%}")

    print(json.dumps(report, ensure_ascii=False, indent=2))

    if args.sql_out:
        with open(args.sql_out, "w", encoding="utf-8") as fh:
            fh.write("START TRANSACTION;\n" + "\n".join(statements) + "\nCOMMIT;\n")
        print(f"\nSQL 已写入 {args.sql_out}（{len(statements)} 条）")

    if args.apply:
        sql = "START TRANSACTION;\n" + "\n".join(statements) + "\nCOMMIT;\n"
        out = subprocess.run(
            [MYSQL, f"-u{USER}", f"-p{PASSWORD}", "--default-character-set=utf8mb4", DB],
            input=sql, capture_output=True, text=True, encoding="utf-8", errors="replace",
        )
        if out.returncode != 0:
            print("写库失败:", out.stderr[:2000])
            return 2
        print(f"\n已写入 {len(statements)} 条生成价格")
    return 0


if __name__ == "__main__":
    sys.exit(main())
