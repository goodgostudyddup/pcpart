import pymysql
cfg=dict(host="127.0.0.1",port=3306,user="root",password="root",database="pcpart",charset="utf8mb4")
con=pymysql.connect(**cfg); cur=con.cursor()
cur.execute("SHOW TABLES"); tables=[r[0] for r in cur.fetchall()]
print("TABLES:", tables)
parts=[t for t in tables if t.startswith("part_")]
for t in parts:
    cur.execute(f"SELECT COUNT(*), SUM(price_usd IS NULL), SUM(price_cny IS NULL), SUM(price_cny=0), SUM(price_usd<=0) FROM `{t}`")
    total,n_usd,n_cny,z,le0=cur.fetchone()
    print(f"{t:20s} total={total:6d} null_usd={n_usd} null_cny={n_cny} cny_zero={z} usd_le0={le0}")
con.close()
