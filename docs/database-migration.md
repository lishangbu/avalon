# 数据库结构与初始化

当前 PostgreSQL 结构位于 avalon-app/src/main/resources/db/schema.sql，包含表、视图、字段注释、索引、唯一约束、外键和检查约束。
Liquibase 唯一入口为 db/changelog.yaml，通过 sqlFile 执行该结构，不包含旧版本升级、业务数据回填或兼容表。

## 初始化

创建空数据库后执行 changelog.yaml，再启动应用。local 和 test profile 自动执行初始化；生产发布流程显式执行，应用保持 spring.liquibase.enabled: false。
Hibernate 使用 validate 检查实际结构，不在运行时自动建表。DATABASECHANGELOG 与 DATABASECHANGELOGLOCK 由 Liquibase 管理，负责防止重复初始化和并发执行。

## 维护与验证

修改结构时同步更新 schema.sql、实体和数据库测试。空库启动须通过真实 PostgreSQL 验证，覆盖认证视图、字段注释、索引、约束及业务事务。
当前基线面向空库；结构不同的数据库不自动升级，不清除校验和或伪造初始化记录。
用户数据库和备份由部署者管理；工程清理不执行删除、恢复或重置用户数据库。
