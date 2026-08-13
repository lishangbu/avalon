# 首版范围

Avalon 首版包含独立玩家与管理员安全域、完整游戏资料管理、RustFS Asset、PlayerCharacter、Team、Party、Challenge、PvE/PvP Battle、Bot、高级 BattleFormat、确定性 Battle Engine、RPG World Topology、Location Traversal、Map Discovery、Pending Encounter、Checkpoint、Battle 历史、回放校验和异步分析。

首版数据直接来自 PostgreSQL 实时关系表，主体使用启用状态控制可用性；不建立 Content Pack、Runtime Catalog、全局 revision 或运行时发布包。结构由 Ent Schema 定义，首套资料由单一 SQL 基线导入，资料变更通过停机维护完成。

后台任务使用 PostgreSQL Outbox + Asynq/Valkey，任务管理、动态调度、失败处置和分析投影均在范围内。RPG 持久化基础表保留，但完整职业、等级、经验、世界探索和成长玩法不作为当前发布门槛。

明确不引入多活 Runtime、事件溯源、完整 CQRS 框架、动态脚本、通用工作流、通用 CRUD 引擎、多语言资料模型或预防性全持久层缓存。Battle 只恢复已提交状态版本和同事务随机游标；历史分析只依赖 Battle 权威事实。

首次发布必须从空 PostgreSQL、已恢复图片的 RustFS 和 Valkey 完成可重复初始化，生成契约、单元测试、真实 PostgreSQL 集成测试、浏览器 E2E、构建与差异检查全部通过。
