# 数据、事务与集成基线

## 1. 本地事务基线

本地事务只负责一个上下文、一个数据库中的一致性。

适用场景：

- 同一上下文内的聚合写入
- 同一上下文内的同步编排
- 使用 `Reactive SQL Client + Kotlin Coroutines` 包裹的本地事务边界

禁止期待本地事务自动覆盖：

- 跨上下文副作用
- 数据库与消息系统的双写一致性

## 2. Outbox 基线

只要一个业务操作在写库后还需要通知其他上下文，就优先使用 outbox。

标准模式：

1. 在同一个本地事务里更新业务表
2. 在同一个本地事务里插入一条 `outbox_event`
3. 事务提交后由 `dispatcher` 异步发布
4. 发布成功后标记已发送，失败则重试

收益：

- 避免双写不一致
- 便于未来接 Kafka/RabbitMQ
- 在单体阶段也能保持跨上下文边界清晰

以下场景通常不需要 outbox：

- 同一上下文内部的纯同步流程
- 不涉及跨上下文通知的本地写操作

## 3. 安全基线

安全基线固定为：

- 认证与授权能力当前由本地 `IdentityAccess` 实现
- 登录标识支持用户名、邮箱、手机
- `access token` 使用短时效 `JWT`
- `refresh token` 使用服务端保存的 `opaque token`
- 并发登录控制通过 `user_session` 实现
- 本地 `IdentityAccess` 负责用户、角色、权限、菜单、认证日志与会话管理

默认建议：

- 使用 Quarkus 官方 JWT 与安全扩展能力
- 不在第一阶段实现完整 OAuth2 / OIDC 授权服务器
- 认证细节以 `modules/identity-access` 中的实现和代码注释为准

## 4. API 与持久化规则

### 4.1 固定默认基线

- REST 层只暴露 DTO，不直接暴露实体
- 默认外部 HTTP API 基线固定为 `Quarkus REST`，不默认使用 `RESTEasy Classic`
- 默认错误响应基线固定为 RFC 9457 Problem Details，成功响应不强制包统一信封
- 写模型和查询模型分离
- 默认持久化基线固定为 `Vert.x Reactive SQL Client + Kotlin Coroutines`
- 如果当前需求确实引入 Redis 能力，默认基线固定为 `quarkus-redis-client + ReactiveRedisDataSource`
- 默认不引入 Hibernate / Panache
- 核心领域模型不要带持久化注解，也不要依赖 `Uni/Multi`
- SQL-first repository 统一放在 `infrastructure`
- 优先向上层暴露 `suspend` 边界，不要把 reactive 类型默认扩散到核心用例
- 一旦引入 Redis，优先在基础设施层用 Kotlin 协程封装 Redis 调用，不默认把 `Uni/Multi` 暴露到核心应用层

### 4.2 局部可选项

- `Vert.x` 默认负责内部异步协作、`Event Bus`、实时推送和高并发内部链路
- `Reactive Routes` 仅作为局部可选项，不作为默认替代整个外部 API 层的方案
- 除非明确运行在可接受阻塞的边界，否则不要在主请求链路中默认使用阻塞式 `RedisDataSource`

### 4.3 默认禁止项

- 不要把持久化行模型或数据库 DTO 直接当接口 DTO
- 不要把 `Uni/Multi` 默认扩散到核心应用层
- 不要在主请求链路里随意混入阻塞式 Redis 访问

## 5. Native 兼容基线

- `native` 支持是项目必达目标，但优先级仍低于性能优化
- 默认技术选型必须优先选择 Quarkus 主路径、对 `native` 友好的扩展与实现方式
- 新引入基础设施依赖、序列化方案、客户端或运行时组件时，默认先确认它是否支持 Quarkus `native`
- 不要默认依赖运行时反射、重动态代理、字节码魔法或脱离 Quarkus 主路径的集成方式
- `Reactive SQL Client`、`Quarkus REST + suspend` 作为当前项目基线，默认应保持 `native-friendly`
- 如果当前任务引入 Redis，则 `ReactiveRedisDataSource` 仍是默认的 `native-friendly` 方案
- Kotlin 数据类的 JSON 序列化和反序列化在 `native` 下要格外谨慎，必要时补显式注解、默认值和验证
