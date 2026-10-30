# -*- coding: utf-8 -*-
"""统计真实锚点的单位价格分位，为生成价提供箍价区间。"""
import sys
import pandas as pd

sys.stdout.reconfigure(encoding="utf-8")
D = r"C:\Users\yxc\Desktop\up\pcpart\.tmpbuild\pricing"
FILES = {"cpu": "cpu", "gpu": "gpu", "motherboard": "motherboard", "ram": "ram",
         "storage": "storage", "psu": "psu", "cooler": "cooler", "pc_case": "pc_case"}

SPECS = {
    "ram": ("capacity_gb", ["ram_type"]),
    "storage": ("capacity_gb", ["storage_type"]),
    "psu": ("wattage", ["efficiency"]),
    "gpu": ("memory_gb", ["memory_type"]),
    "cpu": ("cores_total", ["manufacturer"]),
}
for cat, (den, segs) in SPECS.items():
    df = pd.read_csv(rf"{D}\c_{FILES[cat]}.tsv", sep="\t", dtype=str, keep_default_na=False, na_values=["NULL"])
    df["p"] = pd.to_numeric(df["price_cny"], errors="coerce")
    df["d"] = pd.to_numeric(df[den], errors="coerce")
    sub = df[(df["p"] > 0) & (df["d"] > 0)].copy()
    sub["u"] = sub["p"] / sub["d"]
    print(f"\n== {cat} 元/{den}  总体集 p5/p25/p50/p75/p95 = " +
          "/".join(f"{sub['u'].quantile(q):.2f}" for q in [0.05, 0.25, 0.5, 0.75, 0.95]))
    for seg in segs:
        for name, g in sub.groupby(seg):
            if len(g) >= 15:
                print(f"   {str(name)[:26]:<26} n={len(g):<4} p5/p50/p95 = " +
                      "/".join(f"{g['u'].quantile(q):.2f}" for q in [0.05, 0.5, 0.95]))

for cat in ["cooler", "pc_case", "motherboard"]:
    df = pd.read_csv(rf"{D}\c_{FILES[cat]}.tsv", sep="\t", dtype=str, keep_default_na=False, na_values=["NULL"])
    df["p"] = pd.to_numeric(df["price_cny"], errors="coerce")
    sub = df[df["p"] > 0]
    print(f"\n== {cat} 绝对价 p5/p25/p50/p75/p95 = " +
          "/".join(f"{sub['p'].quantile(q):.0f}" for q in [0.05, 0.25, 0.5, 0.75, 0.95]))
    seg = "form_factor" if cat in ("pc_case", "motherboard") else "water_cooled"
    for name, g in sub.groupby(seg):
        if len(g) >= 15:
            print(f"   {str(name)[:30]:<30} n={len(g):<4} p5/p50/p95 = " +
                  "/".join(f"{g['p'].quantile(q):.0f}" for q in [0.05, 0.5, 0.95]))
