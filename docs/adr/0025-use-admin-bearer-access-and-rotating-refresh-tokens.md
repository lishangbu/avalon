# 玩家与管理端使用 Bearer access token 与轮换 refresh token

## 状态

已接受。

## 背景

玩家与管理员使用独立 Bearer 安全域和 Valkey Session Store。认证协议需要统一适配 gRPC Metadata，并支持短期访问凭据和可撤销设备会话。

## 决策

- 管理端 access token 是十分钟有效的 Ed25519 JWT，通过 `Authorization: Bearer` 传递，只保存在客户端内存。
- JWT 只包含管理员账号、当前会话、设备会话族、签发时间、失效时间和 `access` token 类型，不携带角色或权限声明。
- admin server 公开 JWKS；当前单进程密钥在启动时生成，重启会使未过期 access token 提前失效，refresh token 不受影响。
- refresh token 仍是 256 位随机 opaque token，数据库只保存领域隔离 SHA-256 摘要。每次 refresh 原子撤销旧记录并在同一会话族建立新记录。
- 登录建立采用 staged activation：Valkey 先写入一分钟有效且不可认证的 pending 会话，PostgreSQL 再在单一事务中提交账号登录保护状态、安全版本与成功审计；管理员安全域同时写入登录尝试。提交成功后才激活 Valkey 会话并建立账号、会话族和会话索引。
- 暂存、数据库提交或激活结果不确定时都按 token 摘要幂等撤销；补偿使用脱离 RPC 取消信号且具有硬超时的独立上下文。撤销暂时不可用时，pending TTL 负责最终清理。数据库提交后的激活失败不会向客户端发放 refresh token。
- 已轮换 refresh token 再次出现时视为重放，立即撤销整个会话族。
- Web 将 refresh token 保存在 localStorage，通过 gRPC 请求体显式提交；access token 只保存在内存。localStorage 不可用时降级为内存会话。

## 后果

管理员会话撤销不会立即使已经签发的短期 access token 失效，最长暴露窗口为十分钟；refresh 会立即失败。客户端必须串行合并并发 refresh，续期失败后清空认证状态并返回登录页。
