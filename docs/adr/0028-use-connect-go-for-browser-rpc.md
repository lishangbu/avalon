# 0028：使用 Connect Go 提供浏览器 RPC

状态：生效

## 决策

浏览器 RPC 使用 Connect Go Handler，原生客户端和服务间调用继续使用 Kratos gRPC Transport。Connect Handler 与原生 gRPC 共享同一份 Protobuf 契约和业务服务；浏览器管理端使用 Connect 浏览器 Transport，并通过显式 Bearer Metadata 认证。

HTTP 监听仅承载 Connect RPC。需要双向 Battle 实时操作时使用明确的原生 WebSocket 消息协议。

## 取舍

Connect Go 由同一 Handler 支持浏览器和原生 RPC。Connect Handler 与 Kratos HTTP Transport 组合后，日志、恢复、限流和 Bearer Middleware 由 Kratos 统一执行。

## 生成边界

生成的 `*_connect.go` 文件使用 simple API，必须通过 Buf 生成命令更新。Unary RPC 直接复用业务服务；唯一的 Server Streaming RPC 只保留必要的流接口桥接，不复制业务逻辑。前端代理路径统一为 `/connect`，配置字段统一为 `connect_address`。
