# 模块布局

## 1. 根结构

除非有很强的理由，否则默认使用下面的项目形态：

```text
apps/
  avalon-app/
modules/
  shared-kernel/
  shared-application/
  shared-infra/
  identity-access/
  catalog/
  player/
  battle/
```

## 2. 标准上下文模块布局

bounded context 模块统一使用下面的目录结构：

```text
<context>/
  src/main/java|kotlin/
    <base-package>/<context>/
      domain/
      application/
      infrastructure/
      interfaces/
      projection/
  src/main/resources/
  src/test/java|kotlin/
```

## 3. 各层意图

- `domain`：聚合、实体、值对象、领域服务、领域事件、`repository port`
- `application`：用例、命令处理器、查询处理器、事务编排、ACL 入口
- `infrastructure`：SQL-first repository 实现、行映射、外部适配器、消息、缓存、安全适配器
- `interfaces`：REST 资源、请求/响应 DTO、WebSocket 端点、传输映射
- `projection`：读模型、projection 更新器、查询侧持久化

## 4. Shared 模块

`shared-kernel`：

- 只保留稳定、业务中性的抽象
- 例如：`DomainEvent`、`AggregateRoot`、标识符、基础领域异常

`shared-application`：

- 只保留多个上下文复用的应用层公共契约
- 例如：分页查询模型、时间源契约等不绑定 HTTP、SQL、Quarkus runtime 的用例层类型
- 不放领域内核概念、HTTP DTO、SQL helper、运行时实现或上下文专属命令/查询

`shared-infra`：

- 只保留多个上下文复用的技术基础设施
- 例如：Flyway 基线、Redis 配置、outbox runtime、HTTP/SQL 适配、tracing、时间源实现、ID 生成器

## 5. 放置规则

- 代码只要依赖 Quarkus，就不属于 `domain`
- 代码如果只属于一个上下文，就留在该上下文内，即使未来别的上下文也可能复用
- 代码如果只是传输层关注点，就放在 `interfaces`
- 代码如果只是为了优化读取，就优先放在 `projection`
- `shared-application` 中的契约必须保持纯 Kotlin，不依赖 Quarkus、REST、SQL client、Redis 或具体上下文模块

## 6. `interfaces/http` 组织方式

当某个上下文的 HTTP 入口、DTO、局部
mapper、局部校验和异常映射开始变多时，按业务能力分包，而不是按技术文件类型全局平铺。详细约定见 [interfaces-http-layout.md](interfaces-http-layout.md)。

- `Resource`、DTO、mapper、局部校验尽量放在同一个能力包里
- `common` 只放跨能力复用的 HTTP 约定
- 迁移时优先保持 API path 不变，只调整内部包名
