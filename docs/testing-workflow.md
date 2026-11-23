# 测试流程

发布流程先执行 npm ci 和前端类型检查、生产构建，再运行 Maven clean verify（仅设置 skip.frontend=true，复用刚构建的前端，不跳过后端测试）。设置 AVALON_PACKAGED_CONTENT=1，任务预算为45分钟。验证成功后由 Buildx 构建并推送镜像，本地仍可使用 Maven 的 docker 配置构建镜像。本地设置 AVALON_PROCESS_RECOVERY=1 可额外启用进程恢复校验。

Maven 依赖按所有 pom.xml 和 Wrapper 配置生成缓存键，版本变化时回退到同系统、同 JDK 的下载缓存；验证后、镜像发布前保存。Node 版本读取 mise.toml，npm 下载缓存按前端锁文件管理，每次仍执行 npm ci。Docker 使用独立的 avalon-docker 缓存范围，导出中间层以复用 Spring Boot 分层提取结果。缓存均不包含 target 或 node_modules，不用旧产物替代测试或构建。线上耗时证据和后续复测口径见[发布耗时核查](publish-performance-audit.md)。

## 本地制造回归

从仓库根目录执行 node scripts/test-manufacturing.mjs <模式>。脚本使用 Maven Wrapper，跳过前端构建，不修改游戏规则；Java 环境可通过 mise exec -- node ... 选择。

| 模式 | 验证范围 |
| --- | --- |
| batch | 单件与批次对照、升级边界收缩、工具报废和请求重放 |
| inventory | 材料储备、正式回收和丢弃、资产归属、过期快照、收藏地点与封印球缺额 |
| supply | 跨专业供料、工具递归补造、已有库存交付、满包整理和采集资源恢复 |
| quick | 供料与库存两个专项类 |
| integration | 在上述专项之外增加采集流程和 E009 新制造交付校验 |

例如：node scripts/test-manufacturing.mjs integration。追加 --dry-run 只输出命令。脚本拒绝未知模式和选项，并保留 Maven 失败退出码。原 full 模式及长路线日志汇总脚本已删除。

## 覆盖边界

| 业务风险 | 当前回归 |
| --- | --- |
| 开篇接取、到达与重放 | StoryOpeningQuestRuntimeTest |
| 四职业起步、两城顺序、任务前置和阶段衔接 | QuestOpeningRuntimeTest、QuestNomRuntimeTest、QuestMiddleCitiesRuntimeTest、QuestFireRuntimeTest、QuestEndgameRuntimeTest |
| 采集派遣、资源约束与结算 | GatheringFlowTest |
| 批次结算、工具补造、跨角色交付与库存复用 | ManufacturingSupplyFlowTest |
| 库存容量、材料储备和正式资产操作 | ManufacturingInventoryTest |
| E009 必须新制造、图纸保留和交付幂等 | LifeCraftingFlowTest#trialBlueprintCraftsCompleteArmorAndOnlyFreshProductionCompletesCommission |
| 多专业合作与制造输入 | LifeCooperationTest |
| 成长规则与生产网络闭合 | LifeProgressionTest、LifeEconomyClosureTest |

制造专项显式准备等级、资金、任务资格或原料，再通过正式命令验证目标行为。供料助手不再自动刷级；夹具缺少专业资格时立即失败。采集仍有有限重试，资源恢复、工具损耗和市场扣费仍由正式服务执行。确定性时钟和随机替身用于隔离相应分支，不表示实际随机游玩成本。

数小时的新账号自然成长测试及其专用剧情助手已移除。短测试可以验证关键规则和阶段衔接，但不能证明新账号在实际金币、随机掉落和累计消耗下能连续完成全部内容。这项完整游玩证据需要人工验收或后续独立设计的验证方案；历史执行结果见[主线后路线核查](postgame-route-audit.md)。

快速成长估算可从仓库根目录执行 `node scripts/simulate-natural-growth.mjs`，或传入 `--trials 1000 --seed 20261025` 固定样本范围。脚本直接读取正式任务经验、等级门槛、加权野外遭遇等级及战斗经验和金币公式，以脚本随机数模拟巡逻成长；不启动应用、数据库或浏览器。当前假设剧情任务全部完成、每场胜利击败两名敌人，不模拟战斗胜率、治疗补给、失败、捕捉、采集制造、掉落或行走。因此结果用于快速比较经验与巡逻收益，不作为自然可达性或经济通过证据；正式服务行为仍由专项集成测试验证。

后期供料及试炼补给核算使用 `node scripts/audit-postgame-loop.mjs`，范围、当前缺口与待验证成本见[后期闭环](postgame-loop.md)。该脚本展开正式配方并检查最终原料的采集、种植或钓鱼来源，不启动数据库。

统一入口为 `node scripts/simulate-postgame.mjs`，依次运行模型测试及五类离线报告，直接保存UTF-8 JSON至 `outputs/postgame/report.json`，带内容与模型指纹；不需要启动数据库或浏览器，也不加入正式发布的自然旅程验收。

`node scripts/audit-story-growth.mjs` 核算任务经验与额外巡逻预算，并核对步骤对白中明确写出的“领取/奖励数字经验”是否与该步骤经验效果一致；发现不一致立即失败。此检查不推断无数字对白或后续任务奖励，也不验证战斗可达性。

`node scripts/simulate-postgame-professions.mjs` 输出专业练级直接配方预算和稀有采集的种子样本；`node scripts/simulate-postgame-supply.mjs` 输出成熟四角色网络的工具补造、采集资源、种植、钓鱼、采购与交易税情景。二者当前独立计算，不能相加宣称完整从零经济闭环。关键模型边界和正式目录的来源连通性通过 `node --test scripts/growth-model.test.mjs scripts/production-cost-model.test.mjs scripts/production-ledger.test.mjs` 验证。

`node scripts/simulate-production-journey.mjs` 进一步输出按序制造的库存守恒及练级产物复用，并检查已开放地图的恢复往返道路。缺额仍须由正式供料流程满足；该报告不自动获得资产、地图前置或专业资格。失效的 `NaturalSimulationIT` 已移除，避免调用不存在的 `test:simulation` 命令。

## 执行与报告

修改故事规则时选择任务运行时的对应专项；修改成长或生产成本时同时运行成长与经济闭合测试。提交前运行 ./mvnw clean verify，避免已删除测试的旧编译产物留在 target。需要完整前端及打包校验时不要设置 skip.frontend=true。

测试依赖 Testcontainers 时需启动 Docker。保持当前串行测试安排，不对共享数据库或时钟、随机替身的用例自行开启并行。以进程退出码和 Surefire/Failsafe 报告为通过依据；日志中的中间完成标记不能代替最终结果。发布工作流不上传测试报告或诊断文件，线上构建结果与失败信息通过 GitHub Actions 控制台日志查看。

## 清理后的验证记录

2026-11-17 本地执行 clean verify，设置 skip.frontend=true、AVALON_PACKAGED_CONTENT=1 与 AVALON_PROCESS_RECOVERY=1，结果 BUILD SUCCESS，总耗时9分41秒。各模块及打包测试合计900项，失败0、错误0、跳过12项；其中制造供料14项、库存6项全部通过，进程恢复与打包内容各1项通过。浏览器等条件测试未全部启用，本次也未重新构建前端或发布 Docker 镜像，因此该耗时不能直接视为线上发布耗时。最后仅补充中文注释，并重新执行格式化与 validate，通过。

## 任务与聊天的维护入口

任务运行时回归按开篇、诺姆、中期双城、火城、终局和通用契约拆分。QuestRuntimeTestSupport 只提供共享的注册、前置回执、正式接取与战斗准备，不声明测试方法；Spy 定义集中继承，保持同一 Spring 测试上下文，避免重复启动。跨城顺序和章节衔接断言随对应阶段保留，拆分不表示删除覆盖或开启并行。运行全部相关用例可使用 -Dtest=Quest*RuntimeTest,QuestRuntimeContractTest；从旧版本切换后先执行 clean，避免已删除测试的字节码重复执行。

聊天视图入口为 ChatPanel.vue；useChatSession.ts 管理连接生命周期、战斗门禁、频道草稿、请求防重、历史合并和已读确认，chat-panel.css 保留面板的 scoped 样式。会话模块每次挂载创建独立状态，滚动容器和输入框由视图传入；好友私聊仍经面板暴露的 openPrivate 入口。回归使用 chat.spec.ts 的真实桌面、手机浏览器用例。

2026-11-17 拆分后执行 clean verify，选择六个任务运行时类及 PackagedContentIT，111 项任务回归、1 项打包内容验证全部通过，无失败、无跳过；只有一个任务测试 Spring 上下文启动。76 个测试方法体保持一致，参数化展开总数仍为111。本次未重复运行全项目其他后端用例。前端生产构建、浏览器测试类型检查及修改文件的格式检查通过。

同轮使用本次打包 JAR 和独立 PostgreSQL 容器执行 chat.spec.ts，desktop、mobile 两组均通过（32.4秒），覆盖重连补齐、私聊已读、草稿隔离、战斗隐藏、换号、设置及键盘操作。同步修复旧浏览器夹具：O001 接取后移动到宠物中心再交付，通知断言更新为当前任务名。手机私聊截图复核通过；视口模拟不代表实体手机软键盘验收。

2026-11-23 在依赖升级后重新验证拆分：原任务类的90个测试及辅助方法体均保留，聊天原有26个函数、模板和样式内容保持一致，仅调整模块位置及格式。前端生产构建、浏览器测试类型检查及修改文件格式检查通过。执行 clean verify，选择六个任务运行时类、ChatIntegrationTest、SseTestEventTest，并启用 PackagedContentIT，合计134项全部通过，无失败、无跳过；其中任务回归111项，任务类仅启动一个共享Spring上下文。

聊天后端测试通过 SseTestEvent 读取空行分隔的完整事件，按字段解析并只移除冒号后的一个可选空格，继续精确检查事件名和正文。解析回归覆盖字段顺序、心跳注释、多行正文、有效载荷空白、连续事件和不完整流，避免框架输出空格变化造成误报，也不通过 trim 掩盖正文变化。

同轮以刚构建的JAR和独立PostgreSQL容器执行 chat.spec.ts，desktop、mobile两组均通过（31.6秒），手机私聊截图复核通过。临时应用及数据库容器已清理。本次是上述专项及打包回归，未重复执行全部其他后端测试；手机视口测试仍不代表实体软键盘验收。
