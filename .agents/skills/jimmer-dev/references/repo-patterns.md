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
- generated `View` 只作为读模型；实现和测试里的 update 不要把 `View` round-trip 成 entity 再保存，优先 `UpdateXxxInput.toEntity()` 或 `Entity(existing)`。

## 3. Authorization Module: Management CRUD DTO First, Internal Flows Fetcher First

优先参考以下文件：

- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/controller/RoleController.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/controller/UserController.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/controller/OauthRegisteredClientController.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/repository/AuthorizationFetchers.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/repository/UserRepository.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/repository/MenuRepository.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/MenuServiceImpl.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/RoleServiceImpl.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/OauthRegisteredClientServiceImpl.kt`
- `avalon-modules/avalon-authorization/src/main/dto/Menu.dto`

遵循这些模式：

- 管理端 CRUD：controller/service 契约优先使用 generated `XxxView`、`SaveXxxInput`、`UpdateXxxInput`。
- 管理端读侧：repository 默认补 `pageViews`、`listViews`、`loadViewById`，直接 `fetch(XxxView::class)`。
- 管理端写侧：service 内部仍允许用 entity 做关联绑定、partial merge 与校验，但最终返回值优先 `save(...).let(::reloadView)` / `update(...).let(::reloadView)`。
- 内部授权流程：现有认证、安全、角色装配等直接消费实体图时，继续保留 fetcher 或实体，不要为了“统一”强行改成 dataset 风格。
- repository 仍优先继承 `KRepository`，并把定制查询写成接口默认方法。
- 需要保留“未加载字段”语义的更新流程，才用 `readOrNull` 手工合并新旧实体。
- 对 `User.roles` / `Role.menus`，显式空集合表示清空关联；字段未加载才表示保留 existing，不要把这两种语义合并。

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
- authorization 管理端 CRUD: 优先 generated `View` + generated input + controller `@Valid` + service reload view。
- authorization 内部查询扩展: 先延续 fetcher/entity 或现有专用模型风格。
- 简单 FK 写入: 选扁平 `assocId`，不要嵌套写对象。
- 需要直接拿 FK 值给前端: 先放 DTO，不先加 `@IdView`。
- 只是默认命名的列映射: 不写 `@JoinColumn`。
- 少量自定义查询: 写在 `KRepository` 默认方法里。
