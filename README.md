# PCPart - 个人装机推荐与兼容性校验工具

> 个人写的一个装机辅助小工具。核心是把海外开源硬件库清洗了一遍做成结构化配件库，后端用 Spring Boot 跑一套纯代码的兼容性规则拦截（插槽/限长/限高/供电），前端配了配件增删改查单页，并接入了 Dify Chatflow 来做大白话的预算配置推荐。
> 我将agent运行在我的云服务器上 http://139.196.7.130/pcapp/ 如有想要体验的可以玩一玩
---

## 为什么写这个项目？

自己和朋友装机的时候，最头疼的永远不是“哪个配件性能强”，而是“买回来发现插不上或者塞不下”：
- CPU 选了 LGA1700，主板选了 AM5；
- 显卡买回来 340mm，机箱限长只有 315mm；
- 双塔风冷限高 160mm，小闷罐机箱只给 155mm；
- 电源功率随便买了个虚标的，双烤直接拉闸断电。

网上的配单软件要么广告满天飞，要么规则对不上。于是我决定自己撸一套：
- **数据层**：从 BuildCores open-db 爬取并清洗了 25,000+ 条 8 类核心配件的真实硬件参数；
- **校验层**：在 Java 后端写死了 **R1–R10** 物理兼容性规则（不给大模型胡说八道的机会，硬件尺寸差 1 毫米就是装不进）；
- **AI 推荐层**：把精选硬件向量化灌进 Dify 知识库，配合 Chatflow 工作流解析用户的大白话预算与用途，生成整机备选方案。

---

## 核心功能与架构

### 1. 架构划分（为什么不全扔给大模型？）
大模型擅长“按预算配比出方案”（比如 6000 块 3A 游戏主机显卡占多大比例、CPU 怎么选），但**极度擅长一本正经地胡说八道**（经常把 DDR4 配进 DDR5 主板，或者编造接口）。

因此这个项目严格分层：
- **Dify（云端 Agent / Chatflow）**：只负责意图提取、预算分档、知识库检索推荐候选组合。
- **Spring Boot（本地/业务后端）**：负责配件明细管理、价格回写、以及最终严苛的 **物理兼容性断言**。

```text
用户输入（预算/用途/偏好）
       │
       ▼
 [Dify Chatflow] ── 知识库检索（1200+ 条高频精选配件向量表）
       │
       ▼ 输出候选配置清单
 [Spring Boot 配件管理端] (端口: 8080)
       ├── 配件数据库（MySQL: 2.5万+ open-db 真实参数）
       ├── R1~R10 物理规则引擎（纯 Java 逻辑运算，拒绝幻觉）
       └── 配置单保存 / 查询 / 校验反馈
```

### 2. 核心兼容性规则（R1–R10）
在 `CompatibilityService.java` 中手写实现，涵盖 10 项核心检查：

| 规则编号 | 检查维度 | 核心判断逻辑 |
| :--- | :--- | :--- |
| **R1 / R2 / R3** | CPU & 主板匹配 | 插槽类型完全匹配（如 AM5 / LGA1700）、芯片组支持、超频供电支持 |
| **R4 / R5 / R6** | 内存 & 主板匹配 | 内存代际（DDR4/DDR5）、内存频率上限、最大支持插槽数/容量 |
| **R7** | 显卡 & 机箱物理干涉 | 显卡实测长度 ≤ 机箱最大显卡限长 |
| **R8** | 散热器 & 机箱限高 | 风冷散热器高度 ≤ 机箱风冷限高（水冷检查冷排位） |
| **R9** | 主板版型 & 机箱空间 | 主板尺寸（E-ATX/ATX/M-ATX/ITX）是否属于机箱兼容列表 |
| **R10** | 整机功耗 & 电源瓦数 | (CPU TDP + GPU TDP) × 1.35 ~ 1.5 冗余 ≤ 电源额定功率 |

### 3. 数据来源与清洗
- 原始数据来自海外 `BuildCores open-db`，包含 CPU、主板、内存、显卡、散热器、机箱、电源、固态 8 个品类。
- 自写 Python 脚本对原始脏数据进行清洗（剔除异常年份、缺失核心物理尺寸的无用废品），整理出 24,744 条可用元数据入库 MySQL。
- 另按插槽/接口全覆盖原则，抽取出 1,219 条高频代表款作为大模型知识库（`配件CSV-精选/`）。

---

## 技术栈

- **后端开发**：Java 17 / Spring Boot 3.x / MyBatis-Plus
- **数据库**：MySQL 8.0（配置单存储、8 类硬件参数大宽表）
- **AI 编排**：Dify（Chatflow 工作流 + 混合检索知识库）
- **前端页面**：Thymeleaf + 原生 JS（极简响应式管理单页，无花哨脚手架）
- **数据管道**：Python 3.12（open-db 数据迁移、清洗与价格抓取回写脚本）

---

## 目录结构

```text
pcpart/
├── db/                                # 数据库结构与初始化脚本
│   ├── pc_part.sql                    # 8类配件基础数据与主表结构
│   ├── migration_*.sql                # 增量字段与配置单扩展迁移
│   └── price_engine.py                # 价格解析与统计处理脚本
├── 配件CSV-精选/                       # 喂给 Dify 知识库的精选数据（1219条全接口覆盖）
│   ├── cpu_处理器.csv
│   ├── motherboard_主板.csv
│   └── ...
└── pcpart-assembler/                  # Spring Boot 核心工程
    ├── pom.xml
    ├── scripts/                       # 数据导入与抓取脚本
    └── src/main/java/com/pcpart/assembler/
        ├── compatibility/             # R1~R10 物理规则与结果实体
        ├── controller/                # 8大配件 CRUD 与配置单 RESTful 接口
        ├── entity/                    # 配件及配置单实体类
        ├── internal/                  # 内部供 Agent 调用的过滤与详情接口
        └── service/                   # 业务逻辑与兼容性校验服务
```

---

## 本地跑起来

### 1. 环境准备
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- （可选）Dify 账号或私有化部署实例（若仅测试配件管理与兼容性规则，直接起 Java 即可）

### 2. 初始化数据库
新建数据库 `pcpart`，导入基础结构和迁移表：
```bash
mysql -u root -p -e "CREATE DATABASE pcpart DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p pcpart < db/pc_part.sql
mysql -u root -p pcpart < db/migration_add_id_and_build_config.sql
mysql -u root -p pcpart < db/migration_add_price_columns.sql
```

### 3. 配置与启动后端
修改 `pcpart-assembler/src/main/resources/application.yml` 中的 MySQL 连接密码：
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/pcpart?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: YOUR_PASSWORD
```

进入目录打包并运行：
```bash
cd pcpart-assembler
mvn clean spring-boot:run
```
服务启动后，浏览器访问：`http://localhost:8080/` 即可看到配件管理与配单校验页面。

---

## 目前做完了什么 & 还在折腾什么

- [x] 8 类电脑配件 2.5万条开源数据入库与模糊检索
- [x] Java 后端手写 R1–R10 核心硬件物理兼容性检查引擎
- [x] 前后端单页支持自由勾选配件并一键出具兼容性报告（红标报警 / 绿标通过）
- [x] Dify Chatflow 工作流实现意图分词与预算分流推荐
- [ ] 部分特异性硬件兼容规则补齐（如厚水冷排与高马甲内存的物理挤压判定）
- [ ] 计划将 Java 校验引擎打包为标准 OpenAPI / MCP 工具，让大模型在生成配置单前自主调用反思修正
