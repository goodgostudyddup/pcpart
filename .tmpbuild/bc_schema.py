import pymysql
cfg=dict(host="127.0.0.1",port=3306,user="root",password="root",database="pcpart",charset="utf8mb4")
con=pymysql.connect(**cfg); cur=con.cursor()
cur.execute("SHOW CREATE TABLE build_config")
print(cur.fetchone()[1])
cur.execute("SELECT COUNT(*) FROM build_config"); print("build_config rows:", cur.fetchone()[0])
con.close()
