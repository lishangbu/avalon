# Jimmer ORM Primitives

本文件补充 Jimmer 的通用 ORM/DSL 能力。

- 先按 `SKILL.md` 判断 `dataset` 走 DTO-first 还是 `authorization` 继续 fetcher/entity mix。
- 只有当 `repo-patterns.md` 和 `query-recipes.md` 还不够回答“底层怎么写”时，再使用这里的能力说明。

## 1. Core Semantics

- Jimmer 实体使用 `interface` 定义，而不是 `class`。
- 实体是动态不可变对象，未赋值属性与显式 `null` 是两套语义。
- 由 KSP 在编译时生成实现，因此实体、DTO、fetcher 相关代码都依赖生成结果。

这也是当前仓库强调 Input DTO 和 `readOrNull` 边界的根本原因。

## 2. Entity Mapping Primitives

### 2.1 Relationship Mirrors

- `@OneToMany` 必须作为 `@ManyToOne` 的镜像，并通过 `mappedBy` 指回关联属性。
- `@ManyToMany` 需要区分主动端与镜像端；只有默认中间表或列名不满足现有 schema 时，才补 `@JoinTable`。
- `@JoinColumn`、`@Table`、`@Column` 也只在默认命名不满足数据库实际结构时使用。

示例：

```kotlin
@Entity
interface BookStore {
    @OneToMany(mappedBy = "store")
    val books: List<Book>
}

@Entity
interface Book {
    @ManyToOne
    val store: BookStore?
}
```

### 2.2 Naming Defaults

- 实体名默认映射为表名，如 `BookStore -> BOOK_STORE`。
- 属性名默认映射为列名，如 `firstName -> FIRST_NAME`。
- 多对一属性默认映射为外键列，如 `store -> STORE_ID`。

当前仓库已经采用这套默认命名优先策略，因此看到多余注解时应先怀疑是否可以删除，而不是继续复制。

## 3. DTO Language Basics

- 一个 `.dto` 文件只 `export` 一个实体。
- 生成包路径跟随实体包，DTO 与 `by` DSL 扩展也会跟着实体包组织。
- 实体字段本身可空时，input 字段不要再为了“可选”重复加 `?`。
- `@FetchBy` 依赖 companion fetcher 时，对应 companion 值要显式声明为 `Fetcher<T>`。

这些规则不会改变当前仓库“dataset 优先 generated view，authorization 视情况保留 fetcher”的大方向。

## 4. DSL Query Primitives

### 4.1 Dynamic Predicates

优先使用动态谓词处理可选筛选参数，减少手写 `if`/`when` 拼条件：

```kotlin
sql
    .createQuery(Book::class) {
        where(table.name `ilike?` name)
        where(table.price.`between?`(minPrice, maxPrice))
        select(table)
    }.execute()
```

常见动态谓词包括：

- `eq?`
- `ilike?`
- `between?`
- `gt?` / `ge?` / `lt?` / `le?`

### 4.2 Association Path Join

- 可以直接沿关联路径写条件，例如 `table.store.name`。
- 同一关联路径被多次引用时，Jimmer 会自动合并 join。
- 未真正参与条件或投影的 join 会被忽略。

示例：

```kotlin
sql
    .createQuery(Book::class) {
        where(table.store.name `ilike?` storeName)
        where(table.store.website `ilike?` storeWebsite)
        select(table)
    }.execute()
```

### 4.3 Implicit Subquery For Collection Associations

对一对多或多对多集合做筛选时，优先使用集合关联隐式子查询：

```kotlin
sql
    .createQuery(Role::class) {
        where += table.menus {
            name `ilike?` keyword
        }
        select(table)
    }.execute()
```

- 这类语法会生成 `EXISTS` 子查询。
- 父子关联条件由 Jimmer 自动补齐。
- 同一集合关联上的多个子查询会自动合并。

### 4.4 Fetcher / by DSL

当调用方必须消费实体图，而 generated view 又不是当前场景的主契约时，可以使用 fetcher：

```kotlin
val roles =
    sql
        .createQuery(Role::class) {
            select(
                table.fetchBy {
                    allScalarFields()
                    menus {
                        allScalarFields()
                    }
                },
            )
        }.execute()
```

- 这类能力更适合当前仓库的 `authorization` 模块。
- 对 `dataset` 基础 CRUD，仍优先 `table.fetch(XxxView::class)`，不要因为 fetcher 更灵活就回退。

## 5. Quick Use Order

1. 先读 `SKILL.md` 的模块决策。
2. 再用 `repo-patterns.md` 找仓库锚点。
3. 再用 `query-recipes.md` 复用已有套路。
4. 只有还缺底层 ORM/DSL 语法时，才回到本文件。
