# 任务系统验收

范围为[任务系统规则](quest-system.md)及当前正式 BQ001～BQ066 的接取、推进、交付、记录与阶段对白。这里不把任务框架完成等同于50级内容、游戏经济平衡或真人手机试玩完成。

## 规则与证据

| 规则 | 验证入口 | 覆盖内容 |
| --- | --- | --- |
| 类型、标签与一次性策略 | `QuestRulesCatalogTest`、`QuestConfigurationTest` | 引用、非法配置、主支线默认放弃策略、配置新编号无需增加专用代码 |
| 显式接取、历史隔离 | `QuestAcceptanceIntegrationTest`、`BattleFlowTest` | 未接任务与放弃期间不记专属目标；新捕获与登记分别完成；回放旧捕获来源不补记 |
| 放弃与重接 | `QuestAcceptanceIntegrationTest`、`QuestParticipationPersistenceTest` | 保留有效目标，状态版本防止旧请求误伤重接，原请求重试不重新激活或重复发补给 |
| 无限支线与共享行为 | `QuestAcceptanceIntegrationTest` | 同时接取55条支线；一次行为推进多条有效任务；未追踪任务仍推进 |
| 消耗与发奖 | `QuestAcceptanceIntegrationTest`、`OpeningQuestTest` | 两项任务不能抵扣同一份库存；并发、回滚、换请求号及回执重放不重复发奖；满包转待领 |
| 接取、目标与交付地点 | `QuestAcceptanceIntegrationTest`、`QuestDeliveryRequirementsTest` | 独立接取入口、不传送、拒绝远程交付；即时库存及出战伙伴来源与位置校验 |
| 团队与个人归属 | `DungeonRewardTest`、`QuestAcceptanceIntegrationTest` | 实际参战且已接任务者计入；未行动、未接取及远程成员不计入；角色隔离与结算回滚 |
| 永久事实与任务目标 | `QuestAcceptanceIntegrationTest`、`NaturalExtendedStoryTest` | 已掌握操作和技能保留；战斗、捕捉、场景目标按有效任务期间计入 |
| 解锁与可达性 | `SceneActionCatalogTest`、`AcademyTrainingFlowTest`、`WorkshopCourseFlowTest` | 正向道路与任务解锁环、未知场景引用、消费场景重复归属；前置课程交付后开放后续课程 |
| 必要补给恢复 | `BattleFlowTest` | 捕获任务完成前耗尽金币与封印球，仍可普通野战获得金币并购买封印球 |
| 归档与追踪 | `QuestJournalTest`、`QuestParticipationTest`、`QuestAcceptanceIntegrationTest` | 并行步骤全部完成后归档，主支线各一条追踪，显式取消持久化，未来标题不泄露 |
| 阶段对白 | `QuestDialogueCatalogTest`、`QuestAcceptanceIntegrationTest` | 全部66步骤配置完整；服务端按阶段选择；纯阅读不改状态；重接、首次交付和再访区分 |
| 完整剧情推进 | `BrightOpeningQuestRuntimeTest`、`NaturalExtendedStoryTest` | 四职业真实接取、场景行动、训练、战斗及交付，完成全部66步骤后继续野战达到15级 |
| 手机与桌面操作 | `FormalOpeningBrowserIT`、`GardenBrowserIT`、`RowanBrowserIT` | 同源页面真实操作；开篇30步骤、园圃场景、罗温流程、日志筛选与追踪、阶段交谈 |

集成测试使用隔离 PostgreSQL。支线策略通过独立测试内容验证：当前正式13组任务都有必经前置依赖，均为主线，不把其中某组随意改为可选支线。

可达性校验覆盖当前正式内容使用的正向前置与道路门槛。当前正式内容没有 `closedByFact` 关闭门槛；不能据此声称已证明任意未来关闭条件都可达。

## 执行方式

```bash
AVALON_OPENING_BROWSER=1 \
AVALON_GARDEN_BROWSER=1 \
AVALON_ROWAN_BROWSER=1 \
PLAYWRIGHT_CHROMIUM_EXECUTABLE=/path/to/chrome \
./mvnw -o test '-Dtest=*,!*IT' -Dsurefire.failIfNoSpecifiedTests=false
```

`-o` 适用于依赖已经下载的环境；首次构建可移除。该命令执行后端测试，浏览器及进程恢复验收通过各自环境变量和 Failsafe 入口运行。

JUnit 报告位于各模块 `target/surefire-reports/`；三组浏览器日志位于 `avalon-app/target/` 下的 `formal-opening-browser.log`、`garden-browser.log` 与 `rowan-browser.log`。浏览器截图、失败轨迹位于 `avalon-ui/test-results/`。

## 当前结果

本轮任务系统改造完成。功能回归中内容模块47项、世界模块10项、应用模块344项业务测试通过，共401项。修正受影响流程后的浏览器复验共8项通过：罗温线4项、园圃2项、正式开篇2项，均包含桌面与手机视口。

正式开篇从注册开始完成BQ001～BQ030，验证接取后准备、学院课程解锁刷新、地图突出追踪主线、日志保留其他任务，以及沿相邻道路返乡、恢复并刷新核对持久化生命与魔力。最后一轮开篇浏览器运行约5分钟，Maven构建成功。这些结果来自功能回归与随后三组浏览器复验，不表述为所有测试在同一轮命令中全部通过。

真人手机操作时长、2C4G压力、部署验收、后续等级内容和全游戏经济校准仍各自需要对应验证，本次任务系统验收不替代这些工作。
