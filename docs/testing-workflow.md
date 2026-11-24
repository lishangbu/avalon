# 测试流程

## 构建与发布

项目使用Maven Wrapper。完整验证从根目录执行 `./mvnw clean verify`；Windows使用 `.\mvnw.cmd`。后端专项可使用 `-pl avalon-app -am -Dskip.frontend=true -Dtest=<类或方法> -Dsurefire.failIfNoSpecifiedTests=false test`。跳过前端不能替代最新前端构建和打包验证。

前端在 `avalon-ui` 目录执行 `npm ci`、`npm run build`、`npm run test:typecheck`、`npm run format:check`。浏览器入口为 `npm run test:e2e`，各专项所需环境开关以测试类和Playwright配置为准；依赖Testcontainers时必须运行Docker。

`.github/workflows/docker-publish.yaml` 先构建前端，再设置 `AVALON_PACKAGED_CONTENT=1` 执行 `./mvnw -Dskip.frontend=true clean verify`，通过后构建并推送镜像。作业预算45分钟，缓存下载依赖与Docker构建层，不缓存target或node_modules，也不上传测试报告和诊断文件。线上结果查看Actions控制台；本地报告位于各模块target目录。

## 现有验证入口

| 范围 | 用例 |
| --- | --- |
| 开篇至N023四职业连续路线 | QuestOpeningRuntimeTest |
| 章节规则与前置衔接 | QuestNomRuntimeTest、QuestMiddleCitiesRuntimeTest、QuestFireRuntimeTest、QuestEndgameRuntimeTest |
| 通用任务契约 | QuestRuntimeContractTest |
| 采集、资源与恢复 | GatheringFlowTest、GatheringEnduranceTest |
| 制造与库存 | LifeCraftingFlowTest、ManufacturingSupplyFlowTest、ManufacturingInventoryTest |
| 专业成长与经济规则 | LifeSkillRulesTest、LifeProgressionTest、LifeEconomyClosureTest |
| 副本与阶段奖励 | DungeonRunTest、DungeonRewardTest、DungeonRewardRoutingTest |
| 战斗文本重放 | BattleReplayTest |
| 聊天事务及事件解析 | ChatIntegrationTest、SseTestEventTest |

测试类存在表示有回归入口，不代表本次工作已经执行全部用例。运行后以进程退出码及Surefire/Failsafe结果为准，不以中间日志判定通过。条件浏览器和进程恢复测试只在相应环境开关启用时运行。

## 快速模型与专项脚本

制造入口为 `node scripts/test-manufacturing.mjs <模式>`，模式包含batch、inventory、supply、quick、integration；`--dry-run`只输出命令。模型和其他维护入口见[脚本说明](scripts.md)。

`node scripts/simulate-postgame.mjs` 执行模型测试和当前四类离线报告，保存带内容/模型指纹的UTF-8结果至 `outputs/postgame/report.json`。专业、库存、采集、现金流和道路核算不等于真实战斗及完整新号经济可达性。场景容量、已开放地图和随机样本范围必须随报告一起阅读。

`node scripts/audit-story-growth.mjs` 读取正式经验表与奖励，核算分段巡逻预算并检查对白中的明确经验数字。预算不含完整任务战斗、失败、补给与旅行，不能推导真人升级小时数。

## 验证边界

专项允许以夹具准备等级、资金、地图和原料，验证具体事务、幂等及边界；自然路线必须单独说明注入内容、随机替身及资源消耗。自动化运行耗时不等于游戏时长，手机视口不等于实体手机。共享数据库、时钟和随机替身的测试不自行开启并行。

修改规则后运行受影响专项，内容修改执行目录与引用校验。删除测试或切换旧版本后清理编译产物，避免target残留字节码参与运行。最终交付明确说明执行范围及未验证部分。
