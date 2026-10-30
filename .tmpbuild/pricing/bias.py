# -*- coding: utf-8 -*-
"""诊断：交叉验证里的 实际/预测 中位比（判断是否有系统性低估）。"""
import sys
import numpy as np
import pandas as pd

sys.path.insert(0, r"C:\Users\yxc\Desktop\up\pcpart\db")
import price_engine as pe

sys.stdout.reconfigure(encoding="utf-8")
rng = np.random.default_rng(pe.SEED)

for cat, cfg in pe.CATS.items():
    df = pe.load(cfg["table"])
    price = pd.to_numeric(df["price_cny"], errors="coerce")
    real = df[price.notna() & (price > 0)].copy()
    y = np.log1p(pd.to_numeric(real["price_cny"], errors="coerce").to_numpy(float))
    X, cols, stats = pe.build_design(real, cfg["feats"])
    idx = rng.permutation(len(y))
    parts = np.array_split(idx, 5)
    ratio_all = []
    for k in range(5):
        te = parts[k]
        tr = np.concatenate([parts[j] for j in range(5) if j != k])
        beta = pe.fit_ridge(X[tr], y[tr], 3.0)
        pred = np.expm1(pe.predict(X[te], beta))
        act = np.expm1(y[te])
        ratio_all.append(act / pred)
    r = np.concatenate(ratio_all)
    # 未定价样本的规格是否更弱
    fill = df[~(price.notna() & (price > 0))]
    print(f"{cat:<12} n_real={len(real):<5} 实际/预测 中位={np.median(r):.3f} 均值={r.mean():.3f} "
          f"p25={np.percentile(r,25):.3f} p75={np.percentile(r,75):.3f} | "
          f"待补样本缺年份={pd.to_numeric(fill['release_year'],errors='coerce').isna().mean():.0%}")
