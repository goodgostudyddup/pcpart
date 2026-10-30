import pymysql
cfg=dict(host="127.0.0.1",port=3306,user="root",password="root",database="pcpart",charset="utf8mb4")
con=pymysql.connect(**cfg); cur=con.cursor()
cur.execute("SELECT id,config_name,cpu_id,cooler_id,motherboard_id,ram_id,gpu_id,psu_id,storage_id,case_id FROM build_config")
rows=cur.fetchall()
tblmap={"cpu":"part_cpu","cooler":"part_cpu_cooler","motherboard":"part_motherboard","ram":"part_ram","gpu":"part_gpu","psu":"part_psu","storage":"part_storage","case":"part_pc_case"}
for r in rows:
    print("build", r[0], r[1])
    for i,key in enumerate(tblmap, start=2):
        oid=r[i]
        if not oid: continue
        t=tblmap[key]
        cur.execute(f"SELECT name, price_usd FROM `{t}` WHERE opendb_id=%s",(oid,))
        got=cur.fetchone()
        if got is None: print(f"  {key}: {oid} -> MISSING")
        elif got[1] is None: print(f"  {key}: {oid} -> {got[0]} PRICE NULL")
con.close()
