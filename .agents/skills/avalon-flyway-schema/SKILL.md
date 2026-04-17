---
name: avalon-flyway-schema
description: Use when creating or reviewing Flyway migrations, relational schema changes, table design, indexes, constraints, audit columns, or outbox DDL in avalon.
---

# Avalon Flyway Schema 规则

## 概览

使用这个技能，确保数据库变更遵守项目的 `schema` 归属和 migration 规则。

这个技能聚焦 DDL 和 migration 设计。

## 工作流程

### 1. 先确定 schema 归属

- 阅读 [references/schema-allocation.md](references/schema-allocation.md)
- 在写任何 migration 之前，先确定这张表属于哪个 `bounded context`
- 拒绝“一张表归多个上下文所有”的设计

### 2. 设计表和约束

- 阅读 [references/naming-and-columns.md](references/naming-and-columns.md)
- 一致地应用命名规则
- 有意识地添加索引和唯一约束，不要机械套模板
- 索引应由真实读路径和热路径驱动，不要把“可能以后有用”的索引一股脑加进去
- 拒绝跨上下文强外键

### 3. 在这里处理 outbox DDL

- 在新增或调整共享 outbox 表时，阅读 [references/outbox-ddl-template.md](references/outbox-ddl-template.md)
- 即使运行时使用方式归 outbox 技能负责，DDL 归属也要留在这里

### 4. 把 migration 当作变更集来审查

结束前检查：

1. 目标 `schema` 是否匹配归属上下文
2. 表、索引和约束命名是否符合项目规范
3. 可变聚合表是否包含需要的审计列或版本列
4. join、projection 和 outbox 表是否保持精简，而不是机械继承公共列
5. migration 是否引入了跨上下文强外键
6. 是否为了图省事重复定义近似表结构、近似列语义或重复索引
