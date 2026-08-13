# 安全边界

玩家与管理员均使用短期 Bearer access token 和一次性轮换 refresh token。两类 token 只通过 gRPC Metadata 或显式请求字段传输，不使用 Cookie、CSRF 或 URL 参数。

access token 只保存在客户端内存；Web refresh token 保存在 localStorage，浏览器多标签通过 BroadcastChannel 协调轮换。localStorage 不可用时降级为内存会话，刷新页面后重新登录。refresh token 只保存不可逆摘要，绝不进入日志、审计、错误上报或业务对象。

refresh 每次原子消费旧 token 并创建同一会话族的新 token；重放、退出、账号禁用或绝对 TTL 到期会撤销整个会话族。Valkey 不可用时认证 fail closed。生产部署应启用 AOF 与高可用副本，数据丢失后所有会话重新登录。

服务端不做应用级 Origin 校验，也不开放任意 CORS。生产通过同源 `/connect` 反向代理，开发通过 Vite `/connect` 代理访问 Connect 端口；传输使用 `credentials: omit`。

登录保护使用 Argon2id、渐进锁定和安全审计。日志不得记录密码、access token、refresh token 或其它凭据。
