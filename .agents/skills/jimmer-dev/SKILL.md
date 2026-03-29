---
name: jimmer-dev
description: 当前仓库中的 Jimmer 开发助手。用于设计或修改 Jimmer entity interface、`.dto` 文件、`KRepository` 查询、`SaveMode` 写入流程、关联映射和 fetcher/DTO 取舍。适用于：(1) 新增或重构 dataset/authorization 模块的 Jimmer 模型；(2) 定义 `View`、`SaveInput`、`UpdateInput`、`Specification`；(3) 处理 `@ManyToOne`、复合主键、`@JsonConverter(LongToStringConverter::class)`、`readOrNull` 等仓库约定；(4) 判断何时使用 DTO language、何时保留 fetcher 或实体返回。
---

# Jimmer Dev

## Overview

先按模块判断风格，再按场景选 Jimmer 能力。

- `dataset` 的基础信息 CRUD 默认走 DTO language first。
- `authorization` 目前仍是 fetcher + entity/部分 DTO 的混合风格。除非任务明确要求重构，否则优先保持既有模式。
- 优先复用仓库现有的 `jimmer-spring-boot-starter`、`jimmer-sql-kotlin`、`jimmer-ksp` 和 `KRepository` 基础设施，不要回退到手写映射层。

## Workflow

### 1. 决定读取模型

- 为新的后台 CRUD 页面，优先定义 `.dto` 文件，并生成 `XxxView`、`SaveXxxInput`、`UpdateXxxInput`、`XxxSpecification`。
- 让 repository 直接 `select(table.fetch(XxxView::class))`，不要在此基础上再额外手写输出 DTO。
- 如果现有调用方直接消费实体图，或安全/授权逻辑已经复用 fetcher，继续使用 fetcher，不为“统一”而强行改造。
- 只有在 DTO view 不存在且实体图会被多个内部流程复用时，再新增 manual fetcher。

### 2. 决定实体映射

- 使用 `interface + @Entity`，不要改成 data class 或可变 class。
- 对真实关系优先声明关联属性；把外键 ID 扁平化放到 DTO 边界处理，而不是在实体里同时维护 `assoc` 和 `assocId` 两套状态。
- 默认不要新增 `@IdView` 镜像属性。当前仓库更倾向在 DTO 里使用 `id(assoc) as assocId`，在 specification 里使用 `associatedIdEq(assoc) as assocId`。
- 如果列名已经符合 Jimmer 默认命名，不要保留多余的 `@JoinColumn`。
- 对树形或高度偏管理端的自关联模型，可以保留标量外键。`Menu.parentId: Long?` 是当前仓库允许的例外，不要为了理论完整性强制改成自关联 `@ManyToOne`。
- 对复合主键或桥表，使用 `@Embeddable` + `@PropOverride`。

### 3. 设计 DTO language

- 在 `src/main/dto` 下维护 `.dto` 文件，并以聚合根为单位组织。
- 为简单关联写入使用扁平字段，例如 `id(moveDamageClass) as moveDamageClassId`。
- 为关联筛选使用 `associatedIdEq(...)`，不要让前端传嵌套过滤对象。
- 让 `UpdateXxxInput` 明确包含 `id!`；其余字段保持扁平，不要把简单 FK 更新设计成嵌套对象写入。
- 允许使用 `XxxView` 作为稳定读模型。官方通常认为输出 DTO 不是强制项，但当前仓库的 dataset CRUD 已经把 generated view 作为默认查询契约，这比手写 fetcher 更一致。

### 4. 编写 repository 和 service

- 先继承 `KRepository<Aggregate, Id>`；仅在默认仓储能力不够时再加 DSL 默认方法。
- 把简单定制查询写成 repository interface 默认方法，不要为少量 DSL 额外创建 `*RepositoryExt` 或 impl。
- 写入优先使用 `input.toEntity()` + `save(..., SaveMode.INSERT_ONLY | SaveMode.UPSERT)`。
- 当返回值需要完整关联视图时，保存后按 ID reload `XxxView`。
- 只有在“未加载字段”和“显式 null”必须区分的部分更新流程里，才使用 `readOrNull`。普通可空属性读取不要滥用它。
- 对集合关联替换写入，沿用现有显式保存模式；不要在不知道关联保存策略的情况下随意改动 `AssociatedSaveMode`。

### 5. 收尾检查

- 检查输入输出 JSON 是否仍然是前端使用的扁平结构，尤其是关联 ID 和字符串化 Long ID。
- 检查是否误把简单关联更新改成了嵌套对象写入。
- 检查是否新增了重复镜像字段、无必要 fetcher、无必要 repository 派生类。
- 至少运行受影响模块的编译和相关测试，确认 KSP 生成代码可用。

## References

- 需要当前仓库现有范式和文件锚点时，读取 `references/repo-patterns.md`。
- 需要直接复用仓库里已经落地的查询/写入套路时，读取 `references/query-recipes.md`。
- 需要官方取舍依据时，读取 `references/official-jimmer-notes.md`。
- 若任务涉及更底层的 Jimmer DSL 细节，再补读全局 `jimmer-orm` skill。
