import sys
sys.path.insert(0, r"C:\Users\yxc\Desktop\up\pcpart\db")
import price_engine as pe
import numpy as np, pandas as pd
df = pe.load("part_cpu")
cfg = pe.CATS["cpu"]
X, cols, stats = pe.build_design(df, cfg["feats"])
print("train cols", X.shape, len(cols))
Xn = pe.design_new(df, cfg["feats"], stats, cols)
print("new cols", Xn.shape)
from collections import Counter
c = Counter(c.split("_",1)[0] for c in cols)
print(c.most_common(10))
print([x for x in cols if x.startswith("core_family_")][:5])
print([x for x in cols if x.startswith("socket_")][:5])
