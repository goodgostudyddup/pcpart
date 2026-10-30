# -*- coding: utf-8 -*-
"""抽查生成价的排序合理性：最贵/最便宜各若干条，附关键规格。"""
import re
import sys
import pandas as pd

sys.stdout.reconfigure(encoding="utf-8")
D = r"C:\Users\yxc\Desktop\up\pcpart\.tmpbuild\pricing"
SQL = sys.argv[1] if len(sys.argv) > 1 else rf"{D}\backfill.sql"

pat = re.compile(r"UPDATE (\w+) SET price_usd=([\d.]+), price_cny=([\d.]+), price_updated_at='[^']*' WHERE id=(\d+)")
rows = [(m.group(1), int(m.group(4)), float(m.group(3))) for m in (pat.search(l) for l in open(SQL, encoding="utf-8")) if m]
gen = pd.DataFrame(rows, columns=["table", "id", "cny"])

FILES = {"part_cpu": "cpu", "part_gpu": "gpu", "part_motherboard": "motherboard", "part_ram": "ram",
         "part_storage": "storage", "part_psu": "psu", "part_cpu_cooler": "cooler", "part_pc_case": "pc_case"}
VIEW = {
    "part_cpu": ["name", "cores_total", "threads", "boost_clock", "release_year"],
    "part_gpu": ["name", "chipset", "memory_gb", "memory_type"],
    "part_motherboard": ["name", "chipset", "form_factor"],
    "part_ram": ["name", "ram_type", "capacity_gb", "speed_mt"],
    "part_storage": ["name", "storage_type", "capacity_gb", "form_factor"],
    "part_psu": ["name", "wattage", "efficiency"],
    "part_cpu_cooler": ["name", "water_cooled", "radiator_size", "height"],
    "part_pc_case": ["name", "form_factor", "volume_l"],
}
for table, f in FILES.items():
    real = pd.read_csv(rf"{D}\c_{f}.tsv", sep="\t", dtype=str, keep_default_na=False, na_values=["NULL"])
    real["id"] = real["id"].astype(int)
    g = gen[gen.table == table].merge(real, on="id", how="left")
    cols = VIEW[table]
    print(f"\n===== {table}  n={len(g)} =====")
    for label, sub in (("最贵", g.nlargest(4, "cny")), ("最便宜", g.nsmallest(3, "cny"))):
        print(f"  {label}:")
        for _, r in sub.iterrows():
            spec = ", ".join(f"{c}={r[c]}" for c in cols[1:])
            print(f"    ¥{int(r['cny']):>7}  {str(r['name'])[:48]:<48} [{spec}]")

