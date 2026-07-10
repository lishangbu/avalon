---
name: liquibase-postgresql
description: "Liquibase and PostgreSQL database work in Avalon. Use for changelog YAML, changesets, schema design, tables, columns, constraints, indexes, PostgreSQL SQL, CSV seed data, migration ordering, database baselines, or migration tests."
---

# Liquibase 与 PostgreSQL

## 修改前

- 先检查 `db.changelog-master.yaml`、已有 changeset、Jimmer 映射和迁移测试。
- 领域数据或 seed 变化先读 `CONTEXT.md` 与相关 ADR。
- 默认采用 schema evolution；只有明确重做基线时读取 [references/rebaseline.md](references/rebaseline.md)。

## 项目要求

- Liquibase 是数据库结构与正式 seed 的唯一事实来源。
- 默认新增内聚 changeset，不修改可能已经执行的 changeset。
- 表列使用 `lower_snake_case`；约束命名使用 `pk_`、`uk_`、`idx_`、`fk_` 约定。
- 每表声明主键；字符串声明长度；非空字段具备可靠来源或回填路径。
- Service 判重的业务键必须有唯一约束；外键、高频筛选和排序字段评估索引。
- 优先结构化 YAML；只有 PostgreSQL 特性或复杂批量操作才使用 SQL，并写清数据库假设。
- CSV 表头、null 表达、布尔值和装载顺序必须与 schema 一致；所有外键和 code 引用可解析。
- Current Game Data 不恢复版本、世代、历史、来源 URL 或同步状态；Support Data 必须有可执行规则引用和测试。

## 测试驱动

- 先添加能在空 PostgreSQL 上失败的迁移断言。
- 增量迁移同时覆盖上一已发布状态到当前状态；基线分支覆盖完整重建。
- 约束变化覆盖合法数据与数据库拒绝路径。

## 完成标准

- Testcontainers PostgreSQL 空库迁移通过。
- 增量分支的升级路径通过；显式基线分支报告数据库必须重建。
- schema、Jimmer 实体、Repository 查询和 seed 完全一致。
- 没有孤立外键、重复 code、未命名约束或失效规则引用。
