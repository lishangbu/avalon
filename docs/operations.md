# 运维说明

每个进程分别监听原生 gRPC 与 Connect 地址。浏览器只访问 Connect，生产由同源 `/connect` 反向代理，开发由 Vite 代理到 Connect 端口。HTTP 不承载 REST 业务 API。

管理员和玩家使用显式 Bearer 认证。refresh token 的建立、轮换和撤销由独立 Valkey Session Store 管理；新登录会话在 PostgreSQL 成功提交前保持 pending 且不可认证，异常残留最多保留一分钟。Valkey 就绪检查失败时服务启动失败，运行中故障时认证 fail closed。生产启用 AOF、复制和托管高可用，恢复后要求重新登录。

Connect Transport 的 `GET /metrics` 暴露 Prometheus 指标。`avalon_authentication_login_session_failures_total` 按 `domain` 和 `stage` 区分管理员/玩家安全域以及 `stage`、`commit`、`activate`、`abort` 故障；同一故障还会输出 `authentication.login_session.failure` 结构化日志，但不会记录密码、token 或底层错误正文。

三个进程启动时都必须从 PostgreSQL 领取 Snowflake Node Lease。租约每 10 秒续期、总时长 30 秒，并保留两秒安全窗口；领取失败会阻止启动，续租失败会让 readiness 失败并停止同一 Kratos 应用中的 Transport 或 Worker。节点不通过配置、主机名或静态 Worker ID 指定，进程关闭后等待原租约自然过期再复用。

日志使用 Kratos Logger 默认文本格式；请求元数据统一记录 request-id、trace-id、客户端版本和协议来源。recovery 只向客户端返回安全的 Internal 错误，堆栈只写服务端日志。
