---
name: liquibase
description: "Use when working on Liquibase database changes in avalon: changelog YAML files, schema changes, constraints, indexes, seed data, migration ordering, and PostgreSQL migration verification."
---

# Liquibase 数据库变更

## 使用范围

修改 `db/changelog/**/*.yaml`、新增表字段、主键、外键、唯一约束、索引、初始化数据或迁移验证时使用本技能。

## 基本原则

- Liquibase 是数据库结构事实来源。
- 已执行或可能发布过的 changeset 不直接修改；新增 changeset 演进。
- 优先使用结构化 YAML；复杂批量数据或 PostgreSQL 特性可使用 SQL。
- 每个 changeset 表达一个内聚目标。
- 空库初始化和已有库升级都必须可解释、可验证。

## 命名

- 表名和字段名使用 `lower_snake_case`。
- 主键：`pk_<table>`。
- 唯一约束：`uk_<table>__<columns>`。
- 普通索引：`idx_<table>__<columns>`。
- 外键：`fk_<table>__<column>`。

## 表设计

- 每张业务表必须有明确主键。
- 字符串字段给出长度。
- 非空字段必须有可靠来源或回填策略。
- 高频筛选、排序、外键关联字段要评估索引。
- Service 层判重的业务键必须有数据库唯一约束兜底。

## 模块归属

- 安全和 RBAC changelog 归属 `migration`，由 `security` 拥有实体和服务边界。
- 不要重新引入目录、数据导入或战斗相关 changelog。

## 验证

- 用 Testcontainers PostgreSQL 验证空库迁移。
- 同步核对 Liquibase、Jimmer 实体、Repository 查询和测试数据。
