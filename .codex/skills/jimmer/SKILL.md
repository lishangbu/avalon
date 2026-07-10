---
name: jimmer
description: "Jimmer persistence development in Avalon. Use for entity interfaces, KSP generated models, repositories, KSqlClient queries, fetchers, save commands, transactions, CosId Long identifiers, DTO mapping, associations, or persistence integration tests."
---

# Jimmer

## 修改前

- 先读 Liquibase schema、目标实体、Repository、服务与测试，确认数据库事实和运行时映射一致。
- 涉及领域模型时读 `CONTEXT.md`；涉及 schema 变更同时加载 `liquibase-postgresql`。
- 涉及 Identifier 或 JSON 时读取 [references/identifiers.md](references/identifiers.md)，并加载 `jackson-3`。

## 项目要求

- Liquibase 是 schema 权威；Jimmer 不自动建表或改表。
- 实体使用 Kotlin interface 与 KSP；表、列、唯一键、外键、可空性必须与 schema 一致。
- 主键统一为 `Long/bigint`，由 `CosIdLongUserIdGenerator` 生成；运行时新增不手填 id。
- 稳定自然键才使用 `@Key`；展示名、描述和来源标签不得作为 key。
- 无 id 新增使用 `SaveMode.INSERT_ONLY`，已有 id 更新使用 `UPDATE_ONLY`；不要依赖含糊 upsert。
- 简单唯一键访问使用 Repository；组合筛选、分页、权限过滤和统计使用 `KSqlClient` 或明确 SQL。
- 不在 DTO 组装循环触发隐式查询；显式 fetch 或批量读取避免 N+1。
- REST 边界使用 DTO；事务放服务层，Repository 保持薄接口。

## 测试驱动

- 查询和保存行为先写 PostgreSQL Testcontainers 集成测试。
- 关联变化覆盖插入、更新、删除、外键失败与事务回滚。
- 性能优化先保留可重复查询数量或耗时证据。

## 完成标准

- schema、实体、查询、DTO 与测试 fixture 一致。
- KSP 生成与受影响模块测试通过。
- 没有内存分页、真实列表 `findAll().filter`、N+1 或无边界 JDBC。
- schema 变化同时通过 `:migration:test` 与拥有模块测试。
