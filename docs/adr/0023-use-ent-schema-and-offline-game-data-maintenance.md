# 使用 Ent Schema、单一资料 SQL 与停机资料维护

## 状态

已接受，确定 Ent Schema、单一资料 SQL、停机资料维护和 PostgreSQL Outbox + Asynq 的当前实现。

## 决策

- Ent Schema 是 PostgreSQL 表结构的代码权威；开发和测试可启动时 Create，生产执行审查后的 SQL 基线后只读 Validate。
- 首套游戏资料由发布流程管理的单一离线基线导入。
- 游戏资料直接使用实时关系表和 `enabled` 状态，不建立 Content Pack、Runtime Catalog、全局 revision 或在线维护窗口。
- 结构与资料变更通过停止相关进程的停机流程完成；正在运行的 Battle 读取已经冻结的事实，不因资料更新被改写。
- 可靠异步任务使用 PostgreSQL Outbox + Asynq/Valkey，Asynq 仅保存执行副本，任务生命周期、审计和幂等以 PostgreSQL 为准。
- 业务 Repository、Reader、Query 和 Worker 的后台任务、调度、Outbox 生命周期统一使用 Ent Query/Builder；仅在幂等冲突原子认领、审计哈希链等必须依赖 PostgreSQL 专用语义的边界保留参数化技术 SQL，且这些 SQL 仍运行在同一 Ent 事务内。

## 影响

生产部署需要停机编排和 SQL 审查；开发环境可直接通过 Ent Schema 重建。Battle 重演与分析依赖冻结并持久化的权威事实。后台 Worker 与管理 Store 共享同一 Ent 事务边界，避免重复的 SQL 行模型。
