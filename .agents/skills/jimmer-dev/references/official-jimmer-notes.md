# Official Jimmer Notes

以下内容用于做设计取舍时快速对照官方文档，链接均指向 Jimmer 官方文档站。

## 1. DTO Language

参考：

- https://babyfish-ct.github.io/jimmer-doc/zh/docs/object/view/dto-language/
- https://babyfish-ct.github.io/jimmer-doc/zh/docs/object/view/dto-language/input/

结合官方文档与当前仓库现状，采用以下结论：

- DTO language 是当前仓库新 CRUD 的首选，因为它同时覆盖查询视图、输入对象和 specification。
- Input DTO 在 Jimmer 中很重要，因为实体存在“未赋值”和“显式 null”语义，前端 JSON 不适合直接长期绑定到实体模型。
- 官方对输出 DTO 的态度更保守，很多场景只用 fetcher 就够；但当前仓库的 dataset 模块已经把 generated view 当成稳定查询契约，因此仍优先使用 generated view，而不是手写 fetcher 或手写输出 DTO。

## 2. Repository Defaults

参考：

- https://babyfish-ct.github.io/jimmer-doc/zh/docs/spring/repository/
- https://babyfish-ct.github.io/jimmer-doc/zh/docs/spring/repository/default/

结合官方文档与当前仓库现状，采用以下结论：

- 先从 `KRepository` 开始，不要默认拆成 repository 接口 + 自定义扩展实现类。
- 只有当默认仓储方法不够表达查询或保存语义时，再补接口默认方法或更深层自定义。
- 如果只是少量 DSL 查询，优先写接口默认方法，减少样板代码。

## 3. Fetcher vs View

参考：

- https://babyfish-ct.github.io/jimmer-doc/zh/docs/object/view/fetcher/
- https://babyfish-ct.github.io/jimmer-doc/zh/docs/object/view/dto-language/

结合官方文档与当前仓库现状，采用以下结论：

- 对新 CRUD 优先选 generated view。
- 对已有授权/安全逻辑，如果实体图已经是主要内部模型，继续复用 fetcher。
- 不要在 generated view 已经足够的情况下再手写一套 fetcher + output DTO。

## 4. Association IDs, `@IdView`, `@JoinColumn`

参考：

- https://babyfish-ct.github.io/jimmer-doc/zh/docs/mapping/
- https://babyfish-ct.github.io/jimmer-doc/zh/docs/object/view/dto-language/

结合官方文档与当前仓库现状，采用以下结论：

- `@IdView` 是可选便利能力，不是每个关联都必须加。
- 如果 API 只是在输入、查询或输出边界上需要扁平化 FK，优先在 DTO language 中用 `id(assoc) as assocId` 和 `associatedIdEq(assoc)`。
- 只有当同一个运行时实体模型反复需要直接读取 FK 标量，且不希望经过 DTO 层时，再考虑 `@IdView`。
- 只有当列名不符合 Jimmer 默认命名，或关联映射确实需要覆盖默认行为时，再写 `@JoinColumn`。

## 5. Partial Update and Null Semantics

参考：

- https://babyfish-ct.github.io/jimmer-doc/zh/docs/object/view/dto-language/input/
- https://babyfish-ct.github.io/jimmer-doc/zh/docs/object/dynamic/

结合官方文档与当前仓库现状，采用以下结论：

- 对标准后台表单，优先让 Input DTO 明确传输更新字段，避免直接把前端请求绑定为实体。
- 只有在 entity-based merge 流程里，才需要显式处理“未加载”和“null”的差异。
- 当前仓库的 `readOrNull` 是围绕 `UnloadedException` 的项目辅助工具，不应替代正常的 DTO 设计。
