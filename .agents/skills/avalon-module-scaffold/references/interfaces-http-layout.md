# `interfaces/http` 分包规范

当某个 bounded context 的 HTTP 入口、DTO、局部 mapper、局部校验和异常映射开始变多时，优先按“业务能力”分包，而不是按“技术文件类型”全局平铺。

## 核心原则

- 以 `bounded context + capability` 作为分包单位
- `Resource`、请求/响应 DTO、本地 mapper、局部校验、局部异常处理尽量放在同一个能力包里
- `common` 只放跨能力复用的 HTTP 约定，例如 RFC 9457 Problem Details、分页封装、认证上下文、通用参数解析
- `interfaces/http` 只负责传输层装配，不承载业务规则
- 重构目录时优先保持 API path 不变，只调整包名和文件归属

## Kotlin 文件拆分

- 能力包内的 Kotlin 类型继续按职责拆成独立文件，不要默认把所有类塞进一个大文件
- `Resource`、`Mapper`、`ExceptionMapper` 通常各自单独成文件
- 请求 DTO 和响应 DTO 如果生命周期、校验规则或消费方不同，也应拆成不同文件
- 默认按“一个公开顶层类型一个文件”执行；`companion object` 不算额外类型
- 映射函数如果只服务一个类型，应优先贴近该类型放置，而不是集中塞进总的 `Dtos.kt` / `Mapper.kt`
- 只有在映射函数已经明显跨多个 DTO 复用、且数量较多时，才拆成能力内局部 `*Mappings.kt`
- 优先避免 `Dtos.kt`、`Resources.kt`、`Mappers.kt` 这种覆盖整块能力的巨型文件

## 推荐结构

```text
<context>/interfaces/http/
  common/
    exception/
    response/
    pagination/
  <capability-a>/
    <CapabilityA>Resource.kt
    <CapabilityA>UpsertRequest.kt
    <CapabilityA>Response.kt
    <CapabilityA>Mappings.kt
  <capability-b>/
    <CapabilityB>Resource.kt
    <CapabilityB>Response.kt
    <CapabilityB>Mappings.kt
```

如果某个能力的映射很少，更推荐直接贴近 DTO：

```text
<context>/interfaces/http/<capability>/
  <Capability>Resource.kt
  <Capability>UpsertRequest.kt
  <Capability>Response.kt
```

其中：

- 当前 Avalon 基线在 create / update 共用同一传输契约时，优先使用 `UpsertRequest.kt`
- 只有当创建和更新的字段形状、校验规则或调用语义明显不同，才拆成 `CreateRequest.kt` / `UpdateRequest.kt`
- `UpsertRequest.kt`、`CreateRequest.kt` 或 `UpdateRequest.kt` 可直接放该请求的 `toDraft()`、`toCommand()`
- `Response.kt` 可直接放该响应的 `toResponse()`
- 只有当同文件映射已明显增多时，再抽成 `<Capability>Mappings.kt`

## Avalon 示例

- `catalog/interfaces/http/move`：`MoveResource`、`UpsertMoveRequest`、`MoveResponse`、`MoveTargetResponse`、
  `MoveCategoryResponse`、`MoveAilmentResponse`、`MoveLearnMethodResponse`
- `catalog/interfaces/http/species`：`SpeciesResource`、`UpsertSpeciesRequest`、`SpeciesResponse`、
  `SpeciesEvolutionResponse`、`SpeciesMoveLearnsetResponse`
- `catalog/interfaces/http/reference`：`Item`、`Ability`、`GrowthRate`、`Nature`、`TypeDefinition`、`TypeEffectiveness`
- `identity-access/interfaces/http/auth`：`LoginRequest`、`RefreshTokenRequest`、`TokenPairResponse`、`CurrentUserResponse`、
  `AuthSessionResponse`
- `identity-access/interfaces/http/iam`：用户、角色、权限、菜单
- `identity-access/interfaces/http/snapshot`：授权快照

## 不建议的做法

- 把整个上下文的 `Resource.kt`、`Dtos.kt`、`Mappers.kt` 分别堆到三个大目录里
- 按技术类型拆目录，而不是按业务能力拆目录
- 把 `interfaces/http` 做成共享的公共 HTTP 模块
- 在整理目录时顺手把业务职责打散，导致一个能力的入口分散到很多文件夹
- 为了“统一”而把原本职责不同的类型硬塞到同一个文件里
- 默认新增 `Dtos.kt` 作为请求、响应和映射的收纳盒

## 触发条件

- 单个 `interfaces/http` 目录里的文件数量明显增多
- 一个能力的入口、DTO、局部校验、异常映射开始反复一起改
- review 时很难一眼看出这次改动属于哪个业务能力
