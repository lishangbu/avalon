# 本地开发指南

## 环境准备

本地开发需要 Go 1.27 RC3、Docker、Buf 以及 PostgreSQL、Valkey 和 RustFS。三个进程分别读取自己的 YAML 配置，不从环境变量拼接配置，也不支持热更新。

```powershell
Copy-Item config/server/development.example.yaml config/server/development.yaml
Copy-Item config/admin-server/development.example.yaml config/admin-server/development.yaml
Copy-Item config/worker/development.example.yaml config/worker/development.yaml
```

先修改三个文件中的数据库地址、Valkey 地址和管理端 RustFS 凭据。开发配置使用 `DATABASE_SCHEMA_MODE_CREATE`，首次启动会由 Ent Schema 创建结构；生产环境不得使用该模式。

## 数据库与资料

- `ent/schema/` 是表结构和字段注释的唯一代码权威；生成的 `ent/` 文件不要手工修改。
- 首套游戏资料只通过发布流程管理的离线基线导入；它要求当前 Ent Schema 已完成且资料表为空，重复执行到已有资料库会因唯一约束失败。应用不会在启动时自动导入资料。
- `SchemaModeCreate` 已建立的 254 条 Snowflake Node Lease 可以与资料 SQL 的基础设施前置块重合；该前置块只忽略完全相同的租约唯一键，管理员、系统调度和游戏资料仍不允许重复导入。
- 生产部署先执行审查后的结构 SQL，再导入资料 SQL，最后使用 `DATABASE_SCHEMA_MODE_VALIDATE` 启动进程。
- 资料变更采用停机流程，并直接维护当前 Ent Schema 与资料基线。

## 生成与验证

修改 Proto 或 Ent Schema 后，在仓库根目录执行：

```powershell
go generate ./ent
go tool buf lint
go tool buf generate
```

然后运行：

```powershell
go test ./...
go build ./...
go vet ./...
git diff --check
```

需要 PostgreSQL 的测试使用 integration 标签，并由 Testcontainers 启动固定版本测试数据库：

```powershell
go test -tags integration ./internal/worker ./internal/admin/store ./internal/platform/database
```

RustFS 测试统一使用 `rustfs:latest`。生成的 Go、gRPC 和校验文件属于构建产物，不在业务代码中手工维护。

## 进程职责

- `avalon-server`：玩家认证、玩家 gRPC API 和 Battle Runtime；
- `avalon-admin-server`：管理员认证、游戏资料、Asset 和后台任务管理 gRPC API；
- `avalon-worker`：PostgreSQL Outbox 投递、Asynq 消费、动态调度和分析任务。

后台任务的业务状态由 PostgreSQL Ent 实体负责；只有 PostgreSQL 专用的幂等冲突认领、审计哈希锁和结构校验等技术边界允许使用参数化 SQL。
