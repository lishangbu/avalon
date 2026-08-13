# 首版部署

1. 停止全部 Avalon 流量，创建空 PostgreSQL、Valkey 和单一 RustFS Bucket。
2. 从同一发布批次备份恢复 `pokedex/images/**`，核对对象数量、MIME、字节数和 SHA-256。
3. 使用 Ent Schema 生成并审查生产结构 SQL，在 PostgreSQL 18 空库执行；随后只导入同一发布批次的受控资料基线。不要在已有资料库重复执行。
4. 校验关键表计数、node 0 固定资料、254 条 Snowflake Node Lease、固定管理员、系统调度、Asset 元数据与对象引用。
5. 生成 Protobuf、gRPC、Connect 和校验代码，构建同一 Git commit 的三个生产命令。
6. 使用 `DATABASE_SCHEMA_MODE_VALIDATE` 启动 `avalon-server`、`avalon-admin-server` 和 `avalon-worker`。
7. 登录 `admin / 123456` 后立即通过受控数据库运维流程替换生产密码摘要。
8. 烟测 Bearer 认证与 refresh 轮换、资料 CRUD、公开对象读取、Challenge、训练 PvE、Battle、Outbox、Asynq 和动态调度。
9. 确认反向代理只转发 gRPC/Connect 服务，探针和指标仅由部署平台或内网访问后开放流量。

结构或资料更新均使用同一停机流程。失败时恢复发布前 PostgreSQL/RustFS 快照或重建空环境。
