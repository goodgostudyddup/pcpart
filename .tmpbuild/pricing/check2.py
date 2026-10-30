# -*- coding: utf-8 -*-
"""生成价体检：分布对比 + 单位价格 + 预算窗口覆盖。"""
import re
import sys
import numpy as np
import pandas as pd

sys.stdout.reconfigure(encoding="utf-8")
D = r"C:\Users\yxc\Desktop\up\pcpart\.tmpbuild\pricing"
SQL = sys.argv[1] if len(sys.argv) > 1 else rf"{D}\backfill.sql"

FILES = {"part_cpu": "cpu", "part_gpu": "gpu", "part_motherboard": "motherboard", "part_ram": "ram",
         "part_storage": "storage", "part_psu": "psu", "part_cpu_cooler": "cooler", "part_pc_case": "pc_case"}
SPECS = {
    "part_cpu": ["manufacturer", "cores_total", "tdp"],
    "part_gpu": ["memory_type", "memory_gb", "tdp"],
    "part_motherboard": ["form_factor", "chipset"],
    "part_ram": ["ram_type", "capacity_gb", "speed_mt"],
    "part_storage": ["storage_type", "form_factor", "capacity_gb"],
    "part_psu": ["efficiency", "wattage"],
    "part_cpu_cooler": ["water_cooled", "radiator_size"],
    "part_pc_case": ["form_factor", "volume_l"],
}

pat = re.compile(r"UPDATE (\w+) SET price_usd=([\d.]+), price_cny=([\d.]+), price_updated_at='[^']*' WHERE id=(\d+)")
rows = [(m.group(1), int(m.group(4)), float(m.group(3))) for m in (pat.search(l) for l in open(SQL, encoding="utf-8")) if m]
gen = pd.DataFrame(rows, columns=["table", "id", "cny"])
print("生成条数:", len(gen), "| 按表:", gen.groupby("table").size().to_dict())


def load(table):
    df = pd.read_csv(rf"{D}\c_{FILES[table]}.tsv", sep="\t", dtype=str, keep_default_na=False, na_values=["NULL"])
    df["id"] = df["id"].astype(int)
    return df


print("\n===== 分布对比（真实 vs 生成）=====")
merged = {}
for table in SPECS:
    real = load(table)
    real["price_cny"] = pd.to_numeric(real["price_cny"], errors="coerce")
    g = gen[gen.table == table].merge(real, on="id", how="left")
    merged[table] = (real, g)
    rp = real["price_cny"].dropna()
    qs = [0.05, 0.25, 0.5, 0.75, 0.95]
    print(f"{table:<16} 真实 " + "/".join(f"{rp.quantile(q):>7.0f}" for q in qs) +
          f"   →  生成 " + "/".join(f"{g['cny'].quantile(q):>7.0f}" for q in qs))

print("\n===== 单位价格 =====")


def unit(table, num, den):
    real, g = merged[table]
    d = pd.to_numeric(g[den], errors="coerce")
    ok = d > 0
    v = (g.loc[ok, "cny"] / d[ok])
    rr = real.dropna(subset=["price_cny"])
    rd = pd.to_numeric(rr[den], errors="coerce")
    rok = rd > 0
    rv = rr.loc[rok, "price_cny"] / rd[rok]
    print(f"{table:<16} 元/{den:<12} 真实 p25/p50/p75 = {rv.quantile(.25):.2f}/{rv.median():.2f}/{rv.quantile(.75):.2f}"
          f"   生成 p25/p50/p75 = {v.quantile(.25):.2f}/{v.median():.2f}/{v.quantile(.75):.2f}")


unit("part_ram", "cny", "capacity_gb")
unit("part_storage", "cny", "capacity_gb")
unit("part_psu", "cny", "wattage")
unit("part_gpu", "cny", "memory_gb")
unit("part_cpu", "cny", "cores_total")

print("\n===== 预算窗口覆盖（agent 的 target/min/max 规则）=====")
RULES = {"gpu": (0.38, 0.50, 0.44), "cpu": (0.15, 0.22, 0.18), "motherboard": (0.08, 0.12, 0.10),
         "ram": (0.06, 0.10, 0.08), "storage": (0.05, 0.10, 0.06), "psu": (0.05, 0.08, 0.06),
         "cooler": (0.03, 0.05, 0.04), "case": (0.03, 0.05, 0.04)}
CAT_TABLE = {"gpu": "part_gpu", "cpu": "part_cpu", "motherboard": "part_motherboard", "ram": "part_ram",
             "storage": "part_storage", "psu": "part_psu", "cooler": "part_cpu_cooler", "case": "part_pc_case"}
LOGIC = "CASE WHEN price_cny IS NULL THEN 1 ELSE 0 END"
for budget in (4000, 6000, 10000, 15000, 25000):
    print(f"\n 预算 ¥{budget}:")
    for cat, table in CAT_TABLE.items():
        real, g = merged[table]
        lo, hi, tg = (int(budget * RULES[cat][0]), int(budget * RULES[cat][1]), int(budget * RULES[cat][2]))
        prices = pd.concat([real["price_cny"].dropna(), g["cny"]])
        n_in = int(((prices >= lo) & (prices <= hi)).sum())
        print(f"   {cat:<12} 区间 {lo:>5}-{hi:<5} 有价可选 {n_in:>4} 条   (总 {len(prices)})")
