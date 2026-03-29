# Repository Jimmer Patterns

## 1. Build Baseline

- Jimmer 版本当前为 `0.10.6`。
- 统一依赖基线放在 `gradle/libs.versions.toml` 和根 `build.gradle.kts`。
- 运行时与代码生成组合固定为 `jimmer-spring-boot-starter` + `jimmer-sql-kotlin` + `jimmer-ksp` + Jimmer BOM。
- 使用 KSP 的模块继续把 Jimmer BOM 加到 `ksp` 配置里，避免生成期和运行期版本漂移。

## 2. Dataset Module: DTO First

优先参考以下文件：

- `avalon-modules/avalon-dataset/avalon-dataset-model/src/main/dto/Stat.dto`
- `avalon-modules/avalon-dataset/avalon-dataset-repository/src/main/kotlin/io/github/lishangbu/avalon/dataset/repository/StatRepository.kt`
- `avalon-modules/avalon-dataset/avalon-dataset-service/src/main/kotlin/io/github/lishangbu/avalon/dataset/service/impl/StatServiceImpl.kt`
- `avalon-modules/avalon-dataset/avalon-dataset-repository/src/main/kotlin/io/github/lishangbu/avalon/dataset/repository/NatureRepository.kt`
- `avalon-modules/avalon-dataset/avalon-dataset-repository/src/main/kotlin/io/github/lishangbu/avalon/dataset/repository/BerryRepository.kt`

遵循这些模式：

- 在 `.dto` 中同时定义 `View`、`SaveInput`、`UpdateInput`、`Specification`。
- 用 `id(assoc) as assocId` 扁平化写入字段。
- 用 `associatedIdEq(assoc) as assocId` 扁平化查询条件。
- repository 默认方法直接 `select(table.fetch(View::class))`。
- service 优先 `save(command.toEntity(), SaveMode.INSERT_ONLY | SaveMode.UPSERT)`。
- 如果返回模型要带关联名称或嵌套对象，保存后再 `loadViewById`。

## 3. Authorization Module: Fetcher/Entity Mix

优先参考以下文件：

- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/repository/AuthorizationFetchers.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/repository/MenuRepository.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/MenuServiceImpl.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/RoleServiceImpl.kt`
- `avalon-modules/avalon-authorization/src/main/dto/Menu.dto`

遵循这些模式：

- 现有授权逻辑直接消费实体图时，继续保留 fetcher。
- repository 仍优先继承 `KRepository`，并把定制查询写成接口默认方法。
- `Menu` 树目前保留 `parentId: Long?` 标量外键；这是显式设计，不要自动改造成自关联实体。
- 需要保留“未加载字段”语义的更新流程，才用 `readOrNull` 手工合并新旧实体。

## 4. Entity Mapping Rules

优先参考以下文件：

- `avalon-modules/avalon-dataset/avalon-dataset-model/src/main/kotlin/io/github/lishangbu/avalon/dataset/entity/Stat.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/entity/Menu.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/entity/OauthAuthorizationConsent.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/entity/OauthAuthorizationConsentId.kt`

默认约定如下：

- 主键通常使用 `@GeneratedValue(generatorType = SnowflakeIdGenerator::class)`。
- 对外暴露的 Long 主键通常加 `@JsonConverter(LongToStringConverter::class)`。
- 关联字段优先直接建模为关联，不在实体层冗余维护 `xxxId`。
- 复合键使用 `@Embeddable`；列名覆盖用 `@PropOverride`。
- 只有当数据库列名偏离 Jimmer 默认命名时，才补 `@JoinColumn`。

## 5. Partial Update Rules

优先参考以下文件：

- `avalon-platform/avalon-jimmer/src/main/kotlin/io/github/lishangbu/avalon/jimmer/support/JimmerPropertyAccess.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/UserServiceImpl.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/RoleServiceImpl.kt`

只在以下情况下使用 `readOrNull`：

- 需要保留未加载属性，不把它误当成显式 `null`。
- 需要基于现有实体做 selective merge。
- 需要读取可能未抓取的关联集合。

以下情况不要使用 `readOrNull`：

- 普通 nullable 字段读取。
- DTO input 已经把字段显式表达清楚的 CRUD 流程。
- 只是为了绕开 `UnloadedException`，但根因其实是 fetcher/view 设计错误。

## 6. Default Decision Table

- 新的 dataset 基础 CRUD: 选 DTO language。
- 现有 authorization 查询扩展: 先延续 fetcher/entity 风格。
- 简单 FK 写入: 选扁平 `assocId`，不要嵌套写对象。
- 需要直接拿 FK 值给前端: 先放 DTO，不先加 `@IdView`。
- 只是默认命名的列映射: 不写 `@JoinColumn`。
- 少量自定义查询: 写在 `KRepository` 默认方法里。
