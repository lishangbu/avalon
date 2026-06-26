---
name: jimmer-persistence
description: "Use when working on Jimmer persistence in avalon: Kotlin entity interfaces, repositories, KSqlClient queries, DTO mapping, CosId Long IDs, Liquibase schema alignment, transactions, PostgreSQL persistence tests, and storage boundaries."
---

# Jimmer 持久化

## 使用范围

用于 `avalon` 后端的 Jimmer 实体、Repository、类型安全查询、保存命令、事务和持久化测试。

## 基本原则

- Jimmer 负责运行时映射、类型安全查询、保存命令和 Repository；Liquibase 是唯一 schema 来源，不启用 ORM 自动建表或自动改表。
- 数据库变更先落到 `migration/src/main/resources/db/changelog/changes`，再同步实体、Repository、服务和测试。
- REST API 不直接返回 Jimmer 实体，必须转换为接口自己的 DTO；内部查询投影可使用 Jimmer DTO/fetcher，但不要把持久化结构暴露给外部。
- 简单按 id、稳定唯一键读取或保存使用 `KRepository`；组合筛选、权限过滤、分页列表、复杂详情和统计使用 `KSqlClient` 或明确 SQL。
- 批量导入先走清晰事务边界；确有性能证据时再设计批量优化路径。

## 实体约定

- Kotlin 实体使用 Jimmer interface，并通过 KSP 生成模型；实体、属性、表名、列名、唯一键和外键必须与 Liquibase 一致。
- 新增 `security` 和 `common-persistence` 相关表默认使用 `Long`/`bigint` 主键，并在实体 id 上使用 `@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)`。
- 当前 RBAC 表默认使用 Long 主键；不要为已移除的目录或战斗表恢复 UUID 主键约定。
- 新增表先看所属模块、现有迁移和聚合边界，再决定 Long 或 UUID；不要机械套用旧技能里的 UUID 默认值。
- 运行时创建 Long 主键实体时不要手填 id，交给 Jimmer + CosId 生成；只有 Liquibase seed、测试 fixture 或迁移固定引用允许显式 id。
- `CosIdLongUserIdGenerator` 只适配 Jimmer `UserIdGenerator`，CosId 本身使用官方 starter 提供的 `__share__SnowflakeId`，不要在 Avalon 里再加一套品牌化 CosId 属性或默认值。
- `@Key` 只用于稳定自然业务键，例如 `clientId`、`keyId`、`username`、`code`；不要给可变展示名、描述或来源标签加 `@Key`。
- OAuth client 的 `tokenSettings`、`clientSettings` 保持拍平列映射，不重新引入运行时配置默认值或 JSON 设置包。
- 只映射当前业务需要的导航关系，避免构造完整对象图。

## 查询约束

- 列表页优先返回精确 DTO，不加载无用字段。
- 分页查询要明确 count 和数据查询形态，避免先查全量再内存分页。
- 不在响应组装循环中触发隐式查询；需要关联数据时显式 fetch 或一次性查询。
- 真实列表、唯一性校验、权限过滤不要使用 `findAll().filter`；只有测试、小型 seed 校验或明确有界集合可以临时使用，并在代码里说明边界。
- 查询能力放在拥有该事实的模块；安全运行时查询放 `security`，系统管理查询放 `system`。
- 事务边界放在应用服务层；Repository 保持薄接口，复杂写入由服务协调。

## 关联表边界

- 关联表有独立生命周期、审计字段、状态字段或需要直接出现在 API/管理端时，建 Jimmer entity。
- 纯绑定关系可以在 service 事务内用明确 SQL/JDBC 维护，但必须把边界写清楚，并覆盖 Testcontainers 测试。
- 不要把跨聚合、跨模块的关联维护隐藏进无关 Repository。

## Spring 装配

- Jimmer Repository 由拥有模块通过 `@EnableJimmerRepositories(basePackages = [...])` 暴露；`app` 只负责装配应用入口，不承载业务查询逻辑。
- 条件功能要让 controller/service/repository 消费方使用同一条件，例如 `backend.security.enabled=true`；避免禁用功能时创建依赖缺失的 bean。
- 公共持久化能力放 `common-persistence`，只提供通用 Jimmer/CosId 适配，不放业务实体或业务默认值。

## 验证

- 存储层新增能力至少跑对应模块测试。
- Jimmer、KSP、PostgreSQL 集成变更要跑 Testcontainers 验证。
- 迁移、实体、Repository 和服务 DTO 必须一起核对。
- 涉及 schema 的变更同时跑 `:migration:test` 和拥有模块测试；提交前跑 `rtk proxy git diff --check`。
- 公共实体、Repository、ID generator、自配置和跨模块持久化边界要补 KDoc，遵守项目内 `source-comments` 技能。

## 官方文档入口

- Jimmer mapping：`https://babyfish-ct.github.io/jimmer-doc/docs/mapping/`
- 关联映射：`https://babyfish-ct.github.io/jimmer-doc/docs/mapping/base/association`
- DTO/fetcher：`https://babyfish-ct.github.io/jimmer-doc/docs/query/object-fetcher/dto/`
- Save command：`https://babyfish-ct.github.io/jimmer-doc/docs/mutation/save-command/`
- Spring Repository：`https://babyfish-ct.github.io/jimmer-doc/docs/spring/repository/`
