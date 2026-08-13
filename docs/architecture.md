# 架构设计

## 系统形态

Avalon 是共享 PostgreSQL 的模块化单体。`avalon-server` 提供玩家 API，`avalon-admin-server` 提供管理 API，`avalon-worker` 负责可靠异步任务；三个进程共享纯技术基础设施，但使用独立账号、会话和 RPC 注册集合。领域模块垂直切分，每张业务表只有一个写入所有者，依赖通过显式构造函数装配。

## 技术栈

- Go 1.27 RC3、Kratos V3 与 kratos-layout V3；
- Protobuf、Buf、Protovalidate 与 gRPC 构建产物；
- Ent Schema、pgx 和 PostgreSQL；
- Asynq/Valkey 与 PostgreSQL Outbox；
- AWS SDK for Go v2 与 RustFS；
- `slog`、OpenTelemetry、Prometheus 和标准库并发原语。

生产入口只转发原生 gRPC 或 Connect；健康状态通过标准 gRPC Health 服务读取，指标通过 OTLP 导出。三个进程均使用显式构造，不使用 Wire 或运行时依赖注入容器。

## 持久化与初始化

Ent Schema 是表结构的代码权威。开发、测试和本地集成环境可在启动时以 `SchemaModeCreate` 创建结构；生产先执行经过审查的仓库 SQL 基线并导入同一发布批次的受控资料基线，再以 `SchemaModeValidate` 只读校验。所有持久实体使用正数 BIGINT Snowflake Identifier；固定资料使用 node 0，三个运行进程分别从 PostgreSQL 的 node 1..254 租约池领取节点，无法续租时停止发号并退出。

PostgreSQL 特殊索引、外键和注释由 `internal/platform/persistence` 的显式 SQL 扩展管理。应用启动校验结构差异，发现差异立即失败，不在健康检查中修改生产结构。

## API 契约

Protobuf 是唯一外部契约权威，gRPC、校验代码和 Connect 绑定均由生成命令重建，禁止手工编辑生成文件。管理端只使用生成的 Protobuf/Connect 类型。

关键写命令携带 `Idempotency-Key` 和资源 `expectedVersion`；同键重放返回首次结果，同键异载荷返回冲突。认证先于默认 Kratos 校验错误边界，管理员和玩家使用独立账号域与 Valkey 会话命名空间，客户端统一显式 Bearer。

## 资料、地图与对战

游戏资料和 RPG World Topology 直接保存在 PostgreSQL 实时关系表中，主体通过 `enabled=false` 停用，不建立 Content Pack、Runtime Catalog、Draft、全局 revision 或运行时资料快照。资料修改要求停机维护，由运维停止相关进程后执行管理 CRUD 或 SQL 基线更新；正在运行的 Battle 只读取已冻结 Snapshot，不会读取被修改的资料。玩家地图通过 `avalon.rpg.v1` 提供 Discovery 裁剪视图，管理员读取完整拓扑与校验报告。

Battle Engine 是不依赖数据库、HTTP、时钟或 goroutine 的纯 Go 状态机。Battle Runtime 串行处理命令并冻结必要的规则、Party/Team、Bot 定义和 Random Source；Battle 从最后提交状态版本与随机游标恢复，历史重放只使用 Battle 已保存事实。

## 异步任务

可靠副作用先与业务事务写入 PostgreSQL Outbox，再由 Worker 投递到 Asynq。Asynq 只保存执行副本，任务参数使用稳定 ID，通用任务生命周期、失败次数、退避、审计和幂等响应以 PostgreSQL 为准。调度 occurrence 通过数据库唯一约束保证幂等，单实例 Worker 的并发由配置显式控制。

## 对象存储

RustFS 使用一个业务 Bucket。对象读取公开，上传、覆盖、删除和列举必须认证；Asset 确认阶段由后端读取并校验 MIME、魔数、尺寸、SHA-256 后才标记 `ready`。对象键保留授权来源层级，实际 WebP 使用 `.webp` 扩展名，数据库只保存对象元数据和引用。

## 命令与查询

采用轻量命令/查询分离。写操作由垂直领域应用服务处理，读取使用专用查询和只读投影；不引入完整 CQRS 框架、Command Bus、Query Bus、事件溯源或独立读写 Schema。不同管理资源不共享领域 CRUD 形状，只共享认证、错误映射、事务、审计、幂等和分页等技术基础设施。
