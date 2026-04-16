# 依赖规则

## 1. 允许的方向

在单个上下文内部：

- `interfaces -> application`
- `application -> domain`
- `infrastructure -> domain`
- `infrastructure -> application`
- `projection -> application`，仅在确实需要读侧编排时使用

避免反向依赖这些方向。

## 2. 跨上下文规则

不要这样做：

- 直接调用其他上下文的 `repository`
- 把其他上下文的持久化行模型或 SQL DTO 导入到自己的领域模型
- 依赖其他上下文的内部基础设施实现

优先使用这些方式：

- 应用服务契约
- ACL 适配器
- projection
- 领域事件与 outbox

## 3. Shared 模块规则

推荐共享依赖方向：

- `shared-application -> shared-kernel` 可选，仅在应用层契约需要稳定领域基线时使用
- `shared-infra -> shared-application -> shared-kernel` 允许，语义是基础设施实现应用层契约
- bounded context 的 `application` 层可以依赖 `shared-application` 契约
- bounded context 的 `infrastructure` / `interfaces` 层可以依赖 `shared-infra` 技术适配

禁止方向：

- `shared-kernel -> shared-application`
- `shared-application -> shared-infra`
- 任一 bounded context 反向依赖另一个 bounded context 的内部 `application` / `infrastructure`

如果代码满足下面任一条件，就不要上移到 shared：

- 仍然携带上下文特定语言
- 变化频率很高
- 只是为了绕过单一用例限制而抽出去

只有在同时满足这些条件时，才考虑上移到 shared：

- 足够稳定
- 被多个上下文复用
- 去掉上下文语言后，抽象仍然干净

### 3.1 Shared 模块选择

- 领域内核公共语言：放 `shared-kernel`
- 应用层公共契约：放 `shared-application`
- HTTP/SQL/Redis/outbox/runtime 等技术适配：放 `shared-infra`
- 具体资源筛选、排序、命令、查询：留在拥有者 bounded context

示例：

- `PageRequest`、`Page<T>`、`ClockProvider`：`shared-application`
- `PageResponse<T>`、Problem Details、SQL 分页 helper、`SystemClockProvider`：`shared-infra` 或具体 `interfaces` /
  `infrastructure`
- `SpeciesPageQuery`、`UserPageQuery`、排序字段白名单：对应上下文的 `application`
