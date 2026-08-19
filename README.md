# Avalon

Avalon 是一个以 Go、Kratos V3 和 PostgreSQL 构建的对战游戏后端，配套独立的 [Web 管理端](../avalon-admin-ui)。玩家 API、管理 API 和后台任务分别由三个进程提供。

## 首版能力

- 用户名密码登录、短期 Bearer access token、可撤销 refresh token 与独立玩家/管理员安全域；
- 游戏资料、BattleFormat、Bot Strategy、Asset 和 RPG 基础资料管理；
- RustFS 认证上传、完整对象校验和公开读取；
- 训练 PvE、Challenge、PvP/PvE Battle、Team Preview、Battle Engine、历史与回放校验；
- PostgreSQL Outbox + Asynq 调度、任务重试、审计和分析投影。

资料直接保存在实时关系表中，使用 `enabled` 控制可用性。不存在 Content Pack、Runtime Catalog、全局 revision 或在线维护窗口；资料修改采用停机维护，历史不承诺按旧资料精确重演。

## 技术栈

- Go 1.27、Kratos V3、kratos-layout V3；
- Protobuf、Buf、Protovalidate、原生 gRPC/Connect；
- Ent Schema、pgx、PostgreSQL；
- Asynq、Valkey、PostgreSQL Outbox；
- RustFS（AWS S3 API）、OpenTelemetry、Prometheus、slog。

## 命令

| 命令 | 作用 |
| --- | --- |
| `avalon-server` | 玩家认证、玩家 gRPC API 与 Battle Runtime |
| `avalon-admin-server` | 管理认证、游戏资料、Asset 与运维 API |
| `avalon-worker` | Outbox 投递、Asynq 消费、调度和分析 |

不提供通用 CLI，也不发布第四个生产命令。Ent Schema 在开发/测试可按配置自动创建；生产只接受空 PostgreSQL，执行审查后的 SQL 结构基线并导入同一发布批次的受控资料基线后，再以 Validate 模式启动服务。

## 快速开始

```powershell
cd avalon
Copy-Item config/server/development.example.yaml config/server/development.yaml
Copy-Item config/admin-server/development.example.yaml config/admin-server/development.yaml
Copy-Item config/worker/development.example.yaml config/worker/development.yaml
go generate ./ent
go tool buf lint
go tool buf generate
go test ./...
go build ./...
```

配置文件中的 PostgreSQL、Valkey、RustFS 地址和凭据必须按本机环境替换。开发配置默认使用 `DATABASE_SCHEMA_MODE_CREATE`；生产必须使用只读挂载的完整配置和 `DATABASE_SCHEMA_MODE_VALIDATE`。默认管理员为 `admin / 123456`，仅用于初始化后立即改密。

## 资料与对象

首套游戏资料通过发布流程管理的单一离线基线导入，包含 node 0 固定 Snowflake Identifier、中文文案、关系数据和 Asset 元数据。图片字节不进入 Git，需从受控备份恢复到唯一 RustFS Bucket 的 `pokedex/images/**` 层级；公开读取只允许 `GetObject`，写入和列举必须认证。

## 验证

```powershell
go generate ./ent
go tool buf lint
go tool buf generate
go test ./...
go build ./...
git diff --check
```

管理端另行执行 `npm run verify`。RustFS 集成测试使用 `rustfs:latest`，真实 PostgreSQL 集成测试需要 Docker。

## 文档

- [架构设计](docs/architecture.md)
- [运行设计](docs/operations.md)
- [首版范围](docs/product-scope.md)
- [首版部署](docs/first-release-deployment.md)
- [领域模型](docs/domain-model.md)
- [本地开发指南](docs/development.md)
- [架构决策](docs/adr/README.md)

## 许可证

项目许可证和第三方依赖声明以仓库根目录文件为准。
