# Avalon 基础约束索引

这份文档是 `avalon` 项目约束的导航入口，不再承载全部细则。

- 仓库根目录的 `AGENTS.md` 只保留入口级摘要
- 本目录下的专题 reference 是详细规则主来源
- 发生冲突时，以对应专题 reference 为准

## 使用方式

先读本文件，再按任务类型补读对应专题：

- 架构边界、模块归属、`shared-*`、`schema`、依赖方向
    - [architecture-boundaries.md](architecture-boundaries.md)
- 本地事务、`outbox`、安全基线、API/持久化基线
    - [data-and-integration-baseline.md](data-and-integration-baseline.md)
- 实现工作流、性能意识、兼容策略、错误处理、注释规范、验证要求
    - [implementation-rules.md](implementation-rules.md)
- 清理策略与通用评审检查
    - [cleanup-and-review.md](cleanup-and-review.md)
- 注释、KDoc、边界 DTO 说明和注释回补
    - `avalon-code-commenting`

## 最小硬约束

- 构建工具固定为 `Gradle Kotlin DSL`
- 当前形态固定为 `模块化单体`
- 主上下文固定为 `IdentityAccess`、`Catalog`、`Player`、`Battle`
- 不允许跨上下文 `repository` 直接调用
- 不允许跨上下文强外键
- 不允许写库加发消息的裸双写；跨上下文通知优先 `local transaction + outbox`
- 不允许把 `shared-*` 模块当杂物箱
- `shared-kernel` 放领域内核公共语言，`shared-application` 放应用层公共契约，`shared-infra` 放技术适配与运行时实现
- 允许 `shared-infra -> shared-application -> shared-kernel`，禁止 `shared-application -> shared-infra`

## 专题主来源

- API、持久化、Redis、安全、本地事务与 outbox 基线，以 [data-and-integration-baseline.md](data-and-integration-baseline.md)
  为准
- 实现工作流、性能意识、兼容策略、错误处理、`deprecated` API
  处理与最小验证，以 [implementation-rules.md](implementation-rules.md) 为准
- 注释、KDoc、边界 DTO 说明和回补策略，以 `avalon-code-commenting` 为准
- 清理策略和总评审入口，以 [cleanup-and-review.md](cleanup-and-review.md) 为准

## 快速选读

- 新建模块、拆分上下文、判断代码放哪里：先读 `architecture-boundaries.md`
- 设计表结构、索引、跨上下文读写边界：先读 `architecture-boundaries.md`，再结合对应 schema 技能
- 处理事件、一致性、异步通知：先读 `data-and-integration-baseline.md`
- 写复杂应用服务、复杂 SQL、做性能取舍：先读 `implementation-rules.md`
- 设计或回补注释、KDoc、边界 DTO 说明：先用 `avalon-code-commenting`
- 清理死代码、删占位文件、删配置、做收敛：先读 `cleanup-and-review.md`
