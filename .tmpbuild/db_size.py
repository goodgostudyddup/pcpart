import pymysql
cfg=dict(host="127.0.0.1",port=3306,user="root",password="root",database="pcpart",charset="utf8mb4")
con=pymysql.connect(**cfg); cur=con.cursor()
cur.execute("SELECT table_name, ROUND((data_length+index_length)/1024/1024,1) AS mb, table_rows FROM information_schema.tables WHERE table_schema='pcpart' ORDER BY mb DESC")
for r in cur.fetchall(): print(r)
con.close()
