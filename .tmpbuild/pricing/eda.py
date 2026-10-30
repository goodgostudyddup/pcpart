# -*- coding: utf-8 -*-
import sys, io, math
import pandas as pd
import numpy as np

sys.stdout.reconfigure(encoding="utf-8")
D = r"C:\Users\yxc\Desktop\up\pcpart\.tmpbuild\pricing"

def load(name):
    df = pd.read_csv(f"{D}\\c_{name}.tsv", sep="\t", dtype=str, keep_default_na=False, na_values=["NULL"])
    return df

for cat in ["cpu", "gpu", "motherboard", "ram", "storage", "psu", "cooler", "pc_case"]:
    df = load(cat)
    for c in ["price_cny", "price_usd"]:
        df[c] = pd.to_numeric(df[c], errors="coerce")
    p = df["price_cny"].dropna()
    print(f"\n===== {cat}  rows={len(df)}  priced={len(p)} ({len(p)/len(df):.1%}) =====")
    if len(p):
        q = p.quantile([0.05, 0.25, 0.5, 0.75, 0.95]).round(0).astype(int).to_dict()
        print("  price_cny 5/25/50/75/95%:", q)
    # numeric correlation with log price
    sub = df[df["price_cny"].notna()].copy()
    sub["lp"] = np.log(sub["price_cny"])
    nums = []
    for c in df.columns:
        if c in ("id", "price_cny", "price_usd"):
            continue
        v = pd.to_numeric(sub[c], errors="coerce")
        if v.notna().sum() >= max(30, 0.5 * len(sub)) and v.nunique() > 2:
            nums.append((c, v.corr(sub["lp"]), v.notna().sum()))
    for c, r, n in sorted(nums, key=lambda x: -abs(x[1]) if pd.notna(x[1]) else 0)[:6]:
        print(f"  corr(log price, {c:<22}) = {r:+.3f}  n={n}")
    # categorical spread
    for c in ["manufacturer", "series", "chipset", "form_factor", "efficiency", "storage_type", "ram_type", "memory_type", "side_panel", "modular"]:
        if c not in sub.columns:
            continue
        g = sub.groupby(c)["price_cny"].agg(["count", "median"])
        g = g[g["count"] >= 20].sort_values("count", ascending=False).head(6)
        if len(g) > 1:
            print(f"  {c}: " + ", ".join(f"{str(i)[:28]} n={int(r['count'])} med={int(r['median'])}" for i, r in g.iterrows()))
