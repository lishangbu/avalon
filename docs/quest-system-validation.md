# 任务系统验收

范围为[任务系统规则](quest-system.md)及当前正式 BQ001～BQ074 的接取、推进、交付、记录与阶段对白。这里不把任务框架完成等同于50级内容、游戏经济平衡或真人手机试玩完成。

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
| 永久事实与任务目标 | `QuestAcceptanceIntegrationTest`、`NaturalExtendedStoryTest` | 已掌握操作和技能保留；战斗、捕捉、场景目标按有效任务期间计入 |
| 解锁与可达性 | `SceneActionCatalogTest`、`AcademyTrainingFlowTest`、`WorkshopCourseFlowTest` | 正向道路与任务解锁环、未知场景引用、消费场景重复归属；前置课程交付后开放后续课程 |
| 必要补给恢复 | `BattleFlowTest` | 捕获任务完成前耗尽金币与封印球，仍可普通野战获得金币并购买封印球 |
| 归档与追踪 | `QuestJournalTest`、`QuestJournalStateTest`、`QuestAcceptanceIntegrationTest` | 并行步骤全部完成后归档，主支线各一条追踪，显式取消持久化，未来标题不泄露 |
| 阶段对白 | `QuestDialogueCatalogTest`、`QuestAcceptanceIntegrationTest` | 全部74步骤配置完整；服务端按阶段选择；纯阅读不改状态；重接、首次交付和再访区分 |
| 完整剧情推进 | `BrightOpeningQuestRuntimeTest`、`NaturalExtendedStoryTest` | 四职业真实接取、场景行动、训练、战斗及交付，完成全部74步骤后继续浅滩野战达到16级 |
| 手机与桌面操作 | `FormalOpeningBrowserIT`、`GardenBrowserIT`、`RowanBrowserIT` | 剧情与训练样本走同源页面，其余训练逐场正式HTTP结算；开篇30步骤、园圃场景、罗温流程、日志筛选与追踪、阶段交谈 |

集成测试使用隔离 PostgreSQL。支线策略通过独立测试内容验证：当前正式14组任务都有必经前置依赖，均为主线，不把其中某组随意改为可选支线。

可达性校验覆盖当前正式内容使用的正向前置与道路门槛。当前正式内容没有 `closedByFact` 关闭门槛；不能据此声称已证明任意未来关闭条件都可达。

## 执行方式

日常完整主线用下方全接口入口。后端规则回归直接选择JUnit；页面交互使用各自Failsafe入口：

```bash
mise exec -- ./mvnw test '-Dtest=*,!*IT' -Dskip.frontend=true -Dsurefire.failIfNoSpecifiedTests=false
# 页面验证先完成前端构建；Chromium路径按本机设置。
AVALON_OPENING_BROWSER=1 AVALON_GARDEN_BROWSER=1 AVALON_ROWAN_BROWSER=1 \
PLAYWRIGHT_CHROMIUM_EXECUTABLE=/path/to/chrome \
mise exec -- ./mvnw test-compile failsafe:integration-test failsafe:verify \
  -Dskip.frontend=true -Dit.test=FormalOpeningBrowserIT,GardenBrowserIT,RowanBrowserIT \
  -Dfailsafe.failIfNoSpecifiedTests=false
```

JUnit报告位于各模块`target/surefire-reports/`及应用`target/failsafe-reports/`；浏览器日志位于`avalon-app/target/`，截图与失败轨迹位于`avalon-ui/test-results/`。命令使用当前代码生成报告，已有报告不能代替本次结果。

## 2026-10-10 港湾团聚增量验证

正式范围现为BQ001～074、14组主线。新增`HarborReunionTest`四职业验证，覆盖资料只读、未接取拒绝、错答纠正、清网后开战、撤退回照护站、战后清理、引导、标牌、登记与奖励防重；库存和幻兽个体仅受正式战斗规则影响，不因团聚消耗或增发。

`NaturalExtendedStoryTest`八条新号路线完成74步骤并通过普通浅滩野战达到16级；本次各需54场追加野战，51～54次恢复，保留为节奏依据，不等同于真人投入结论。完整默认回归581项中580通过、1项既有跳过，零失败、零错误；`HarborReunionBrowserIT`和`PortBrowserIT`各两种视口，共4项浏览器用例通过。日志、后端摘要与自然成长记录见`outputs/harbor-reunion/`。

## 页面自然成长验证

`formal-opening.ts`与`natural-chapter.ts`共用`natural-training.ts`。默认每段训练前两场保留完整页面操作，其余使用页面当前Bearer身份，顺序调用正式移动、恢复、开战及回合接口。每场必须胜利、经验增加并沿相邻道路返回恢复设施；服务端照常处理经验、金币、伙伴成长和资源消耗。训练后刷新页面，核对持久化等级和地点，再继续剧情。任务交付、捕获、剧情战斗、课程和购买穿戴仍走页面，不写测试经验或任务事实。购买装备前，页面优先出售已换下的可售装备；空间仍不足时，仅在新注册的测试账号中按正式确认流程销毁已换下的低等级旧装备，遵守实际背包容量。接口训练由Node直接发HTTP，避免Playwright对数万次重复请求生成庞大trace；页面失败轨迹仍保留，训练过程由逐场JSON记录验收。

保持原有后端八条自然成长路线。浏览器的`natural-training-level*.json`附件包含每场经验前后值、接口战局ID和回合数、页面与接口场数及耗时，`formal-opening-timing`附件记录开篇时长；隔离入口将附件保存在`avalon-app/target/formal-opening-results.json`的JSON报告中。这些是自动化验收耗时，不能当作真人升级速度。

隔离服务运行开篇与15级自然成长（前端已构建、依赖已缓存时可加`-Dskip.frontend=true`和`-o`）：

```bash
AVALON_OPENING_BROWSER=1 \
AVALON_NATURAL_CHAPTER_BROWSER=1 \
PLAYWRIGHT_CHROMIUM_EXECUTABLE=/path/to/chrome \
./mvnw verify '-Dtest=NaturalExtendedStoryTest' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=FormalOpeningBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false
```

省略`AVALON_NATURAL_CHAPTER_BROWSER`只执行BQ001～030开篇。需要排查重复点击的界面行为时，加`AVALON_FULL_UI_TRAINING=1`恢复每场完整页面操作；两种模式使用同一公开战局选招策略和相同正式成长规则。

2026-10-10页面复验：桌面、手机各一个新账号完成BQ001～BQ030、真实野战至15级及七件卫兵装备购买穿戴，两项通过。完整流程分别445.899秒、448.493秒；每个账号四段训练各保留两场页面战斗，其余2137场通过正式HTTP逐场结算。该入口用于页面验收，日常模拟使用下方纯HTTP入口。

后端`NaturalExtendedStoryTest`八条自然路线全部通过；前端测试类型与格式检查、Java导入检查通过。日志、手机和桌面截图、逐场训练记录及摘要位于`outputs/fast-natural-training/`，最终`summary.json`与`browser.log`对应通过结果；中途失败记录另存，不能作为通过证据。

## 全接口日常模拟

日常模拟默认执行公开HTTP长链，与浏览器入口独立。运行：

```bash
scripts/simulate-natural.sh
# 依赖已缓存可加 -o；脚本使用mise中的JDK，不编译前端。
AVALON_SIMULATION_ROUTE=level15 scripts/simulate-natural.sh -o
AVALON_SIMULATION_ROUTE=both scripts/simulate-natural.sh -o
```

脚本通过`NaturalSimulationIT`创建隔离PostgreSQL和随机端口。需要已安装的npm依赖（首次在`avalon-ui`执行`npm ci`），无需Chromium或浏览器可执行路径。默认`story16`从新账号走完BQ001～074并真实野战达到16级；`level15`从新账号完成BQ001～030，沿平原及城郊正式练到15级、购买穿戴七件卫兵装备；`both`依次执行两条路线，各自注册独立账号。

运行中的服务已明确指定时，可在`avalon-ui`中设置`AVALON_BASE_URL`后执行`npm run test:simulation`，仍只操作模拟器自行注册的新账号。模拟器读取内容目录确定动作、课程和选项，寻路使用HTTP返回的当前可见地图；任务接取、交付、捕捉、恢复、每回合、经验及资产全部由正式接口校验和持久化，不注入进度、不固定随机数。背包满时先正式出售闲置装备，不可出售时只清理新测试账号已换下的低等级旧装备。

两条路线共用一个全接口开篇及公开战局选招策略，后续任务读取正式内容步骤和当前接取状态；不是为快进新增生产接口。原有浏览器开篇、完整15级页面流程、页面专项和四职业八条后端自然成长路线保持独立入口。

HTTP日志位于`avalon-app/target/natural-simulation.log`，JSON报告位于`natural-simulation-results.json`。`natural-simulation`附件记录请求数、每步进度、每场战局ID、回合数、胜负、捕获及经验前后值，浏览器操作数应为0。全接口验收覆盖业务与公开HTTP协议，不能替代页面显示、按钮、布局或真人时长的验证。

2026-10-10最终复验：`AVALON_SIMULATION_ROUTE=both scripts/simulate-natural.sh -o`两条独立新号路线全部通过，Maven构建成功。完整74步骤至16级耗时13.588秒，3159次HTTP请求、175场正式战斗，其中浅滩追加54场；30步骤至15级并购买穿戴七件卫兵装备耗时119.673秒，29690次HTTP请求、2166场正式战斗。两条路线均为0次浏览器操作，逐场战局ID唯一，训练经验严格增加，最终任务进度通过服务端日志核对，退出时无活动战局或课程。

同范围15级路线对比上一批混合模拟445.899／448.493秒，本次约快3.74倍；该比较保留真实随机战斗，场数可能略有不同。74步默认路线与30步练级路线范围不同，不能据二者耗时计算加速倍数。两条路线合计模拟约2分14秒，含编译、隔离数据库和服务启动的整条命令约2分35秒。证据位于`outputs/api-natural-simulation/summary.json`、`story16.json`、`level15.json`及对应HTTP、Maven和JUnit报告；初次失败记录单独保留，不计为通过结果。

2026-10-13统一格式复验：14项任务、74步骤、82目标、127项效果完成迁移，原编号、条件、接取地点、对白、补给、奖励与依赖对照通过。后端654项测试无失败（1项跳过），离线目录校验、前端构建及测试类型检查、981个Java文件组织检查通过。新版实例交付接口下，两条独立新号HTTP旅程全部通过；NPC接取与交付、聊天个人通知分别在桌面和手机通过。证据见`outputs/quest-format-migration/`。仅使用隔离测试库，不导入旧任务存档。
