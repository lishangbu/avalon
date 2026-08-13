# ADR-0026：管理端业务统一使用 Protobuf/gRPC

- 状态：已接受
- 日期：2026-08-09

## 决策

业务 RPC 以 `api/proto` 中的 Protobuf 定义为唯一运行契约。服务端在独立原生 gRPC 和 Connect 监听地址注册对应领域服务。

Web 管理端使用生成的 TypeScript 消息和 Connect Client；原生 gRPC 供服务端和未来原生客户端使用。Bearer access token 和幂等键通过 RPC Metadata 传递。

## 取舍

浏览器的 Connect 底层仍然需要 HTTP 传输，这是浏览器能力限制，不是 REST/JSON 接口。原生 gRPC 与 Connect 使用独立监听地址，便于网络策略和故障隔离。
