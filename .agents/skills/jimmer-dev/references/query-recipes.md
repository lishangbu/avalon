# Query And Save Recipes

## 1. Dataset CRUD: View + Input + Specification

适用场景：

- 新增基础信息管理页面
- 后端需要标准列表、详情、创建、更新、删除
- 关联字段主要是简单 FK

推荐做法：

1. 在 `.dto` 中定义 `View`、`SaveInput`、`UpdateInput`、`Specification`
2. repository 用 `table.fetch(View::class)` 返回读模型
3. service 用 `input.toEntity()` + `SaveMode`
4. 写入后如果要返回完整关联信息，再按 ID reload
5. `View` 只做读模型；update 和相关测试不要把 `View` 反向 `toEntity()` 后直接保存

参考文件：

- `avalon-modules/avalon-dataset/avalon-dataset-model/src/main/dto/Stat.dto`
- `avalon-modules/avalon-dataset/avalon-dataset-repository/src/main/kotlin/io/github/lishangbu/avalon/dataset/repository/StatRepository.kt`
- `avalon-modules/avalon-dataset/avalon-dataset-service/src/main/kotlin/io/github/lishangbu/avalon/dataset/service/impl/StatServiceImpl.kt`

示例骨架：

```kotlin
interface XxxRepository : KRepository<Xxx, Long> {
    fun listViews(specification: XxxSpecification?): List<XxxView> =
        sql
            .createQuery(Xxx::class) {
                specification?.let(::where)
                select(table.fetch(XxxView::class))
            }.execute()

    fun loadViewById(id: Long): XxxView? =
        sql
            .createQuery(Xxx::class) {
                where(table.id eq id)
                select(table.fetch(XxxView::class))
            }.execute()
            .firstOrNull()
}
```

```kotlin
override fun save(command: SaveXxxInput): XxxView =
    repository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)
```

注意：

- 对 DTO-first 聚合，`XxxView` 可能只携带部分关联字段；把它 round-trip 成 entity 再 `save`，可能触发非预期的关联 upsert。
- 更新优先使用 `UpdateXxxInput.toEntity()`，或者基于已加载 entity copy 做定向修改。

## 2. Dataset Pagination: Query View Directly

适用场景：

- 管理端表格需要分页
- 分页结果直接展示 DTO view

参考文件：

- `avalon-modules/avalon-dataset/avalon-dataset-repository/src/main/kotlin/io/github/lishangbu/avalon/dataset/repository/BerryRepository.kt`
- `avalon-modules/avalon-dataset/avalon-dataset-service/src/main/kotlin/io/github/lishangbu/avalon/dataset/service/impl/BerryServiceImpl.kt`

示例骨架：

```kotlin
fun pageViews(
    specification: XxxSpecification?,
    pageable: Pageable,
): Page<XxxView> =
    sql
        .createQuery(Xxx::class) {
            specification?.let(::where)
            select(table.fetch(XxxView::class))
        }.fetchPage(pageable.pageNumber, pageable.pageSize)
```

## 3. Authorization Management CRUD: Generated View + Generated Input

适用场景：

- `authorization` 的管理端分页、列表、详情、创建、更新
- 返回值需要稳定读模型
- service 内部仍要保留 entity merge 或关联绑定

参考文件：

- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/repository/UserRepository.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/repository/RoleRepository.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/repository/Oauth2RegisteredClientRepository.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/UserServiceImpl.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/OauthRegisteredClientServiceImpl.kt`

推荐做法：

1. `.dto` 中同时定义 `XxxView`、`SaveXxxInput`、`UpdateXxxInput`、`Specification`
2. repository 默认补 `pageViews`、`listViews`、`loadViewById`
3. service 的 `page/list/getById` 直接返回 generated `XxxView`
4. `save/update` 里先做 entity 绑定或 merge，再 `let(::reloadView)`

示例骨架：

```kotlin
fun pageViews(
    specification: XxxSpecification?,
    pageable: Pageable,
): Page<XxxView> =
    sql
        .createQuery(Xxx::class) {
            specification?.let(::where)
            select(table.fetch(XxxView::class))
        }.fetchPage(pageable.pageNumber, pageable.pageSize)

fun loadViewById(id: Long): XxxView? =
    sql
        .createQuery(Xxx::class) {
            where(table.id eq id)
            select(table.fetch(XxxView::class))
        }.execute()
        .firstOrNull()
```

```kotlin
override fun save(command: SaveXxxInput): XxxView =
    repository.save(bindSomething(command.toEntity()), SaveMode.INSERT_ONLY).let(::reloadView)
```

不要把这条 recipe 扩展到认证、授权装配等内部流程。

## 4. Authorization Internal Query: Fetcher First

适用场景：

- 现有 authorization 模块扩展查询
- 返回值继续使用 entity
- 内部逻辑已经依赖 fetcher 图

参考文件：

- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/repository/AuthorizationFetchers.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/repository/RoleRepository.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/repository/MenuRepository.kt`

示例骨架：

```kotlin
fun listWithMenus(specification: Specification<Role>?): List<Role> =
    sql
        .createQuery(Role::class) {
            specification?.let(::where)
            select(table.fetch(AuthorizationFetchers.ROLE_WITH_MENUS))
        }.execute()
```

不要把这类已有模式硬改成 DTO，除非任务明确要求做授权模块整体收敛。

## 5. Many-To-Many Bind: Preserve Unloaded, Clear Explicit Empty

适用场景：

- `User.roles`
- `Role.menus`
- 更新时需要区分“未传该字段”和“显式清空集合”

参考文件：

- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/UserServiceImpl.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/RoleServiceImpl.kt`

推荐步骤：

1. 如果是 update，先按带 fetcher 的方式加载 existing
2. 用 `readOrNull` 判断请求中的关联集合是否已加载
3. 如果集合已加载，提取关联 ID，回库加载受控实体
4. 如果集合未加载且是 update，回退到 existing 的关联集合
5. 用新的 detached entity 重建写入对象，并在“显式空集合”时主动标记集合为已加载

示例骨架：

```kotlin
val existing =
    if (preserveWhenNull) {
        aggregate.readOrNull { id }?.let(repository::findNullable)
    } else {
        null
    }

val currentRelations = aggregate.readOrNull { relations }
val relationIds =
    currentRelations
        ?.mapNotNull { it.readOrNull { id } }
        ?.toCollection(LinkedHashSet())
val shouldLoadRelations = currentRelations != null
val boundRelations =
    when {
        currentRelations != null && !relationIds.isNullOrEmpty() ->
            relationRepository.findAllById(relationIds)
        currentRelations != null ->
            emptyList()
        preserveWhenNull ->
            existing?.readOrNull { relations } ?: emptyList()
        else -> emptyList()
    }

return Aggregate {
    aggregate.readOrNull { id }?.let { id = it }
    if (shouldLoadRelations) {
        relations()
    }
    boundRelations.forEach { relation -> relations().addBy(relation) }
}
```

注意：

- `readOrNull` 只用于保留未加载语义。
- 不要对 `aggregate.readOrNull { relations }` 直接 `.orEmpty()`；那会把“未加载”和“显式空集合”混成一种情况。
- 只有集合已加载但为空时，才表示“清空关联”；此时要显式调用 `relations()` 把空集合标记为已加载。
- 如果新模块已经是 DTO-first，优先设计明确 input，不要复制这套 merge 流程。

## 6. Composite Key Upsert/Delete

适用场景：

- 桥表
- 复合主键表
- 矩阵单元格或关系表按主键 upsert

参考文件：

- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/entity/OauthAuthorizationConsent.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/entity/OauthAuthorizationConsentId.kt`
- `avalon-modules/avalon-dataset/src/main/kotlin/io/github/lishangbu/avalon/dataset/service/impl/TypeEffectivenessServiceImpl.kt`
- `avalon-modules/avalon-dataset/avalon-dataset-repository/src/main/kotlin/io/github/lishangbu/avalon/dataset/repository/TypeEffectivenessEntryRepository.kt`

推荐做法：

- 用 `@Embeddable` 定义复合 ID
- 用 `@PropOverride` 对列名做覆盖
- service 里显式构造 ID，再做 `save(..., SaveMode.UPSERT)` 或 `deleteById(id)`

示例骨架：

```kotlin
val id =
    XxxRelationId {
        leftId = left.id
        rightId = right.id
    }

repository.save(
    XxxRelation {
        this.id = id
        value = command.value
    },
    SaveMode.UPSERT,
)
```

## 7. Association FK Exposure: Prefer DTO Flattening

适用场景：

- 前端请求只传一个关联 ID
- 前端展示里需要拿到关联对象和关联 ID

参考文件：

- `avalon-modules/avalon-dataset/avalon-dataset-model/src/main/dto/Stat.dto`

优先顺序：

1. 在 input 中用 `id(assoc) as assocId`
2. 在 specification 中用 `associatedIdEq(assoc) as assocId`
3. 在 view 中直接展开关联对象
4. 只有 DTO 边界不足以满足复用时，才考虑 `@IdView`

不要默认在实体里同时暴露 `assoc` 和 `assocId`。

## 8. Save Result Strategy

优先顺序：

- 返回简单标量即可：可直接包装保存结果
- 返回 DTO view 且依赖关联名称或稳定管理端读模型：保存后 reload view
- 返回 entity 且内部继续传递：保留 entity/fetcher 风格

参考文件：

- `avalon-modules/avalon-dataset/avalon-dataset-service/src/main/kotlin/io/github/lishangbu/avalon/dataset/service/impl/AbilityServiceImpl.kt`
- `avalon-modules/avalon-dataset/avalon-dataset-service/src/main/kotlin/io/github/lishangbu/avalon/dataset/service/impl/StatServiceImpl.kt`

判断标准不是“哪种写法更短”，而是返回模型是否稳定、是否需要完整关联信息。
