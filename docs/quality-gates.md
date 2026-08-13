# 质量门禁

## 生成与静态检查

- `go generate ./ent`、Buf lint/generate 必须成功；
- 生成 Go、gRPC、校验和 Ent 代码不得手工编辑；
- `gofmt`、`go vet ./...`、依赖整理和 `git diff --check` 必须通过；
- 前端执行 Protobuf 同步/生成、typecheck、lint、Vitest、bundle build 和 Playwright。

## 测试

领域规则使用表驱动测试、黄金回放和 fuzz；Repository 使用真实 PostgreSQL；RustFS 使用 `rustfs:latest` 验证认证写入、公开读取、匿名写入拒绝和不可变对象；Outbox、Asynq、调度和审计使用真实 PostgreSQL 集成测试。

契约测试必须覆盖 Protobuf 字段、错误码、分页、Bearer Metadata、refresh 原子轮换、安全域隔离和管理端生成客户端。端到端测试不依赖旧维护窗口或全局 revision。

## 数据与发布

空 PostgreSQL 通过 Ent Schema 和单一资料 SQL 完整重建；关键表计数、中文注释、约束、索引、初始管理员、Asset 元数据和对象引用必须一致。所有 Ent Identifier 必须是正数 BIGINT 且没有数据库默认发号，254 条运行期节点租约必须完整；三个进程必须在取得租约后才能提供服务，并在租约失效时停止。生产结构使用 Validate 模式，不在应用启动或健康检查阶段自动修改。

发布 artifact 必须可追溯到 Git commit、Protobuf artifact、Ent Schema、SQL 基线、镜像 digest 和测试报告。禁止通过跳过测试、隐藏开关或手工改库宣称完成。
