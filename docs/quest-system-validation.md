# 任务系统验收

范围为[任务系统](quest-system.md)及300项正式任务、327步骤的接取、推进、交付与阶段对白。当前通过与待办见[实施状态](implementation-progress.md)。

## 规则与证据

| 规则 | 验证入口 | 覆盖内容 |
| --- | --- | --- |
| 类型、标签与一次性策略 | `QuestRulesCatalogTest`、`QuestConfigurationTest` | 引用、非法配置、主支线默认放弃策略、配置新编号无需增加专用代码 |
| 显式接取、历史隔离 | `QuestAcceptanceIntegrationTest`、`BattleFlowTest` | 未接任务与放弃期间不记专属目标；新捕获与登记分别完成；回放旧捕获来源不补记 |
| 放弃与重接 | `QuestAcceptanceIntegrationTest`、`QuestJournalStatePersistenceTest` | 保留有效目标，状态版本防止旧请求误伤重接，原请求重试不重新激活或重复发补给 |
| 无限支线与共享行为 | `QuestAcceptanceIntegrationTest` | 同时接取55条支线；一次行为推进多条有效任务；未追踪任务仍推进 |
| 消耗与发奖 | `QuestAcceptanceIntegrationTest`、`OpeningQuestTest` | 两项任务不能抵扣同一份库存；并发、回滚、换请求号及回执重放不重复发奖；满包转待领 |
| 接取、目标与交付地点 | `QuestAcceptanceIntegrationTest`、`QuestDeliveryRequirementsTest` | 独立接取入口、不传送、拒绝远程交付；即时库存及出战伙伴来源与位置校验 |
| 团队与个人归属 | `DungeonRewardTest`、`QuestAcceptanceIntegrationTest` | 实际参战且已接任务者计入；未行动、未接取及远程成员不计入；角色隔离与结算回滚 |
| 永久事实与任务目标 | `QuestAcceptanceIntegrationTest`、`QuestRuntimeIntegrationTest` | 已掌握操作和技能保留；战斗、捕捉、场景目标按有效任务期间计入 |
| 解锁与可达性 | `SceneActionCatalogTest`、`AcademyTrainingFlowTest`、`WorkshopCourseFlowTest` | 正向道路与任务解锁环、未知场景引用、消费场景重复归属；前置课程交付后开放后续课程 |
| 必要补给恢复 | `OpeningQuestTest` | 四职业开篇资产耗尽后免费恢复，通过真实河岸战斗与掉落重新买球 |
| 归档与追踪 | `QuestJournalTest`、`QuestJournalStateTest`、`QuestAcceptanceIntegrationTest` | 并行步骤全部完成后归档，主支线各一条追踪，显式取消持久化，未来标题不泄露 |
| 阶段对白 | `QuestDialogueCatalogTest`、`QuestAcceptanceIntegrationTest` | 正式目录阶段配置、服务端阶段选择、只读查询、重接与交付重放 |
| 自然成长 | `NaturalExtendedStoryTest`、`StoryOpeningJourney` | 四职业O001～N025、两种两城顺序及火山通路有自然证据；终章、主线后与当前复测边界见实施状态，不注入进度或种子 |
| 手机与桌面操作 | `StoryBrowserIT`、`OpeningBrowserIT`、`PvpBrowserIT` | 当前故事开篇、诺姆分支改选与完成记录在两种视口通过；分支使用前置夹具，完整故事浏览器旅程尚未覆盖 |

集成测试使用隔离PostgreSQL。正式目录已经区分主线与支线；主线、普通支线及周期任务的接取和放弃规则分别验证，不能把目录数量视为自然可玩性证据。

可达性校验覆盖当前正式内容使用的正向前置与道路门槛。当前正式内容没有 `closedByFact` 关闭门槛；不能据此声称已证明任意未来关闭条件都可达。

## 执行方式

执行命令与分层策略见[测试流程](testing-workflow.md)。
