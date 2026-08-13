# Avalon Go 后端协作说明

## 仓库边界

- 本仓库是独立 Git 仓库 `github.com/lishangbu/avalon`；管理端位于相邻的独立仓库 `avalon-admin-ui`。
- 只修改当前任务所需文件；不得覆盖、删除或回退工作树中的无关改动。
- 用户未明确要求时，不执行 commit、push 或外部发布操作。

## 当前技术与产品边界

- 使用 Go 1.27 RC3、Kratos V3、Ent、pgx、Protobuf/Buf、Asynq、PostgreSQL Outbox 和 RustFS；版本事实以 `go.mod` 与工具配置为准。
- API 以 Protobuf 为唯一权威，管理端业务通过原生 gRPC/Connect 访问；生成的 Go、gRPC、Connect 与校验代码都是构建产物，不手工修改。
- 玩家与管理员由不同进程注册不同 RPC 并使用独立账号域；两者均使用短期 Ed25519 Bearer access token 和轮换 refresh token，通过 gRPC Metadata 与显式请求字段传输；系统不使用 Cookie、CSRF、RBAC、OAuth、MFA 或恢复码。
- 游戏资料使用实时关系表和启用状态，不保留发布包、运行时目录、全局 revision 或维护窗口；资料变更通过停机维护完成。
- RustFS 使用单一 Bucket：读取公开，列举和写操作必须认证。
- 生产命令固定为 `avalon-server`、`avalon-admin-server` 和 `avalon-worker`；数据库由 Ent Schema 与仓库维护的 SQL 基线初始化，不恢复通用 CLI 或独立迁移命令。

## 领域语言与架构决定

- 修改代码、测试或设计前，读取根目录 `CONTEXT.md` 和相关 `docs/adr/`。
- 术语、测试名称、注释和对外描述使用 `CONTEXT.md` 中的稳定领域语言。
- ADR 是历史决策记录；新决定取代旧决定时更新 `docs/adr/README.md` 的状态，不静默改写旧 ADR。
- 新术语写入 `CONTEXT.md`；只有难以逆转、代码中不明显且存在真实取舍的决定才新增 ADR。

## 实现与验证

- 领域模块保持垂直切分，不建立全局 `biz`、`data` 或 `service` 层，不引入依赖注入容器。
- 新增或修改的导出类型、结构体字段与关键逻辑必须有准确、完整的简体中文注释。
- 新行为先补测试，避免无关重构；修改 Proto 或 SQL 后通过项目生成命令重建派生产物。
- 按风险运行聚焦测试、`go test ./...`、`go build ./...` 与 `git diff --check`；无法运行外部依赖测试时明确报告未验证范围。

## 协作资料

- GitHub Issues：`docs/agents/issue-tracker.md`
- 分诊标签：`docs/agents/triage-labels.md`
- 领域文档规则：`docs/agents/domain.md`
