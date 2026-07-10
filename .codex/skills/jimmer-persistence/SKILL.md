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
- Jimmer 实体的 `Long` 主键，以及响应 DTO 中的 `id`、`...Id` 长整型标识符，统一标注 `@JsonConverter(LongToStringConverter::class)`，由 Jimmer 负责 JSON 字符串转换；不要把标识符手工 `toString()` 后再塞进字符串 DTO。
- 简单按 id、稳定唯一键读取或保存使用 `KRepository`；组合筛选、权限过滤、分页列表、复杂详情和统计使用 `KSqlClient` 或明确 SQL。
- 批量导入先走清晰事务边界；确有性能证据时再设计批量优化路径。

## 实体约定

- Kotlin 实体使用 Jimmer interface，并通过 KSP 生成模型；实体、属性、表名、列名、唯一键和外键必须与 Liquibase 一致。
- 项目内 Jimmer 实体统一使用 `Long`/`bigint` 主键，并在实体 id 上使用 `@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)`。
- Long 主键由 CosId 雪花算法生成；Liquibase 不得为这些主键声明 `autoIncrement`、identity 或数据库序列。
- 不要为业务表恢复 UUID 主键约定；确需例外时先记录明确的外部协议或迁移约束。
- 运行时创建 Long 主键实体时不要手填 id，交给 Jimmer + CosId 生成；只有 Liquibase seed、测试 fixture 或迁移固定引用允许显式 id。
- 无 id 新增使用 `repository.save(entity, SaveMode.INSERT_ONLY)`，已有 id 更新使用 `SaveMode.UPDATE_ONLY`；不要对无 id、无 `@Key` 的实体调用默认 upsert，也不要使用 Jimmer 已弃用的 `repository.insert/update` 快捷方法。
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
- 纯绑定关系可以在 service 事务内用 Jimmer mutation API 或类型安全 SQL 维护，但必须把边界写清楚，并覆盖 Testcontainers 测试；生产代码不直接使用 JDBC。
- 不要把跨聚合、跨模块的关联维护隐藏进无关 Repository。

## Spring 装配

- Jimmer Repository 由拥有模块通过 `@EnableJimmerRepositories(basePackages = [...])` 暴露；`app` 只负责装配应用入口，不承载业务查询逻辑。
- 条件功能要让 controller/service/repository 消费方使用同一条件，例如 `backend.security.enabled=true`；避免禁用功能时创建依赖缺失的 bean。
- 公共持久化能力放 `common-persistence`，只提供通用 Jimmer/CosId 适配，不放业务实体或业务默认值。

## Jackson 约束

- 项目统一使用 Jackson 3 的 `tools.jackson` mapper、tree model 和 `TypeReference` API，不允许直接使用 Jackson 2 的 `com.fasterxml.jackson.databind`、`com.fasterxml.jackson.core.type` 或 Kotlin module。
- 局部创建的 Jackson 3 mapper 如果需要处理 Jimmer immutable 对象，必须注册 `org.babyfish.jimmer.jackson.v3.ImmutableModuleV3`；不得注册 `ImmutableModuleV2`。
- `com.fasterxml.jackson.annotation` 是 Jackson 3 继续使用的共享注解包，可以用于 `JsonProperty` 等模型注解；它不代表允许引入 Jackson 2 databind。
- Spring Bean 优先注入 Boot 4 已配置的 Jackson 3 `ObjectMapper`；只有模块级回退或隔离测试确实需要独立 mapper 时才使用 `tools.jackson.databind.json.JsonMapper.builder()`。

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
