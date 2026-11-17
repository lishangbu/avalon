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
| 四职业起步、两城顺序、任务前置和阶段衔接 | QuestRuntimeIntegrationTest |
| 采集派遣、资源约束与结算 | GatheringFlowTest |
| 批次结算、工具补造、跨角色交付与库存复用 | ManufacturingSupplyFlowTest |
| 库存容量、材料储备和正式资产操作 | ManufacturingInventoryTest |
| E009 必须新制造、图纸保留和交付幂等 | LifeCraftingFlowTest#trialBlueprintCraftsCompleteArmorAndOnlyFreshProductionCompletesCommission |
| 多专业合作与制造输入 | LifeCooperationTest |
| 成长规则与生产网络闭合 | LifeProgressionTest、LifeEconomyClosureTest |

制造专项显式准备等级、资金、任务资格或原料，再通过正式命令验证目标行为。供料助手不再自动刷级；夹具缺少专业资格时立即失败。采集仍有有限重试，资源恢复、工具损耗和市场扣费仍由正式服务执行。确定性时钟和随机替身用于隔离相应分支，不表示实际随机游玩成本。

数小时的新账号自然成长测试及其专用剧情助手已移除。短测试可以验证关键规则和阶段衔接，但不能证明新账号在实际金币、随机掉落和累计消耗下能连续完成全部内容。这项完整游玩证据需要人工验收或后续独立设计的验证方案；历史执行结果见[主线后路线核查](postgame-route-audit.md)。

## 执行与报告

修改故事规则时选择任务运行时的对应专项；修改成长或生产成本时同时运行成长与经济闭合测试。提交前运行 ./mvnw clean verify，避免已删除测试的旧编译产物留在 target。需要完整前端及打包校验时不要设置 skip.frontend=true。

测试依赖 Testcontainers 时需启动 Docker。保持当前串行测试安排，不对共享数据库或时钟、随机替身的用例自行开启并行。以进程退出码和 Surefire/Failsafe 报告为通过依据；日志中的中间完成标记不能代替最终结果。发布工作流始终上传测试报告，便于定位失败及耗时。

## 清理后的验证记录

2026-11-17 本地执行 clean verify，设置 skip.frontend=true、AVALON_PACKAGED_CONTENT=1 与 AVALON_PROCESS_RECOVERY=1，结果 BUILD SUCCESS，总耗时9分41秒。各模块及打包测试合计900项，失败0、错误0、跳过12项；其中制造供料14项、库存6项全部通过，进程恢复与打包内容各1项通过。浏览器等条件测试未全部启用，本次也未重新构建前端或发布 Docker 镜像，因此该耗时不能直接视为线上发布耗时。最后仅补充中文注释，并重新执行格式化与 validate，通过。
