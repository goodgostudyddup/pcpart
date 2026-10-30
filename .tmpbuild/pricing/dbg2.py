import sys
sys.path.insert(0, r"C:\Users\yxc\Desktop\up\pcpart\db")
import price_engine as pe
import numpy as np, pandas as pd
df = pe.load("part_cpu")
cfg = pe.CATS["cpu"]
price = pd.to_numeric(df["price_cny"], errors="coerce")
train = df[price.notna() & (price>0)].copy()
print("train rows", len(train))
X, cols, stats = pe.build_design(train, cfg["feats"])
print("train X", X.shape, "cols", len(cols))
print(cols)
Xn = pe.design_new(df, cfg["feats"], stats, cols)
print("new X", Xn.shape)
