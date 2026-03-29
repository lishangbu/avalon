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

## 3. Authorization Query: Fetcher First

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

## 4. Many-To-Many Bind: Merge Existing Entity Carefully

适用场景：

- `User.roles`
- `Role.menus`
- 更新时需要区分“未传该字段”和“显式清空集合”

参考文件：

- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/UserServiceImpl.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/RoleServiceImpl.kt`

推荐步骤：

1. 如果是 update，先按带 fetcher 的方式加载 existing
2. 用 `readOrNull` 读取当前请求中可能未加载的属性
3. 提取关联 ID，回库加载受控实体
4. 用新的 detached entity 重建写入对象

示例骨架：

```kotlin
val existing =
    if (preserveWhenNull) {
        aggregate.readOrNull { id }?.let(repository::findNullable)
    } else {
        null
    }

val currentIds = aggregate.readOrNull { relations }?.mapNotNull { it.readOrNull { id } }.orEmpty()
val boundRelations =
    when {
        currentIds.isNotEmpty() -> relationRepository.findAllById(currentIds.toSet())
        preserveWhenNull -> existing?.readOrNull { relations }.orEmpty()
        else -> emptyList()
    }
```

注意：

- `readOrNull` 只用于保留未加载语义。
- 如果新模块已经是 DTO-first，优先设计明确 input，不要复制这套 merge 流程。

## 5. Scalar Tree Model: Keep `parentId`

适用场景：

- 菜单树、分类树
- 前端主要做树展示和表单编辑
- 业务并不依赖复杂自关联抓取

参考文件：

- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/entity/Menu.kt`
- `avalon-modules/avalon-authorization/src/main/kotlin/io/github/lishangbu/avalon/authorization/service/impl/MenuServiceImpl.kt`

推荐做法：

- 实体保留 `parentId: Long?`
- 树组装放在 service 或专门的 tree util
- 父节点合法性校验单独做，不依赖 ORM 自关联图

这类模型不要因为 Jimmer 支持关联，就机械改成 `@ManyToOne` 自关联。

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
- 返回 DTO view 且依赖关联名称：保存后 reload view
- 返回 entity 且内部继续传递：保留 entity/fetcher 风格

参考文件：

- `avalon-modules/avalon-dataset/avalon-dataset-service/src/main/kotlin/io/github/lishangbu/avalon/dataset/service/impl/AbilityServiceImpl.kt`
- `avalon-modules/avalon-dataset/avalon-dataset-service/src/main/kotlin/io/github/lishangbu/avalon/dataset/service/impl/StatServiceImpl.kt`

判断标准不是“哪种写法更短”，而是返回模型是否稳定、是否需要完整关联信息。
