# 测试流程

2026-11-16补充：连续制造旅程现已接入银档之后的地图资格、四角色高阶供料及E009制造交付，但完整自然路线尚未通过验收。`NaturalEndgameCraftingFlowTest`的3项夹具回归与`NaturalManufacturingInventoryTest`的1项整理回归已通过；它们验证工具升级、跨专业交易、重复请求和正式出售规则，不能代替从零成长证据。制造浏览器专项的桌面及移动端6项用例已通过。下一步应完成长路线，并记录采集、工具补造、供料与金币成本。

使用Maven Wrapper与Docker。针对改动选择目录测试、运行时事务测试或自然流程测试，最后执行统一构建。

- 内容修改：运行avalon-content测试，验证Schema、引用、门禁和奖励。
- 任务事务修改：运行QuestRuntimeIntegrationTest及受影响的接取、交付测试。
- 学院修改：运行学院、职业及工坊课程测试。
- 故事流程修改：运行NaturalExtendedStoryTest对应方法，保留实际战斗、捕获和采集规则。
- 页面修改：运行TypeScript检查和相关Playwright用例；服务端使用隔离测试数据库。

自然流程以公开命令推进，不注入任务事实、经验、资源或战斗胜利。允许测试时钟推进自动采集的周期时间，但不能跳过资源消耗、工具损坏与随机产出。测试夹具与自然难度证据分别表述。

## 可复现与隔离

自然流程默认使用基准种子20261025，并以用例显示名派生独立序列。用`-Davalon.test.seed=数字`重跑另一组结果；报告中的`STORY_SEED`记录用例和基准种子。生产种子仍由SecureRandom生成，不修改概率。需要逐步进度时加`-Davalon.test.trace=true`；默认仅记录种子与断言失败信息。

JUnit显式串行执行，Surefire使用单fork。静态测试替身在每条集成用例前后恢复；不得直接开启用例并行，因为真实HTTP线程共享替身，数据库故障注入也会修改共享结构。独立CI job通过各自Testcontainers容器隔离。测试数据库读取最多等待60秒；一般用例默认5分钟，自然流程每条最多12分钟。

多角色制造合作由`naturalSuppliersReachIronToolQualification`连续执行，方法本地超时上限为12小时：三角色开局与制造专业起步、铜工具供给、制皮及供应专业升至5级、铁工具材料合作及制造交易，再推进铁档采集资格、工具按缺额补造、制皮及供应专业7级，最后加入第四名烹饪角色，实际制作铁档口粮和首批银针、银陷阱并交付。上述首批银工具供给链已完整通过，单项6633秒。当前另接入两城剧情、银霜林地解锁、狩猎7级资格及银工具持续补造下的制皮7→9级，扩展运行仍待结果；12小时只是本地诊断上限，不是预期耗时。地图解锁由`naturalLeatherworkerUnlocksSilverRegionWithCooperativeWood`独立验证通过，工匠交易28份任务木材，避免制皮角色学习第四专业。此前拆分的起步、铜档和材料准备方法已合并；单独复现使用`-Dtest=NaturalExtendedStoryTest#naturalSuppliersReachIronToolQualification`。烹饪合作另由`naturalCookingStartsWithGatheredIngredients`执行，允许20分钟，木档起步和三角色供给下的铜档烹饪5级已有独立通过证据。方法的本地超时上限不代表CI预算达标；Actions仍为45分钟，当前长路线不满足该预算，须继续优化验证安排和执行开销。当前结果边界见[主线后路线核查](postgame-route-audit.md)。

完整自然旅程继续保留：它验证实际前置成本和跨章节成长，不能用预置经验或任务事实替代。四职业开篇与双城入口断言已合并到对应完整旅程，去掉五次重复开篇。章节规则和事务回归优先使用现有QuestRuntimeIntegrationTest等局部夹具，开发时只运行受影响的方法；发布仍执行完整verify，不能将局部通过表述为全量通过。每周及手动运行Story seed stress，对三个基准种子分别执行全部自然旅程。

CI无论成功失败均上传Surefire/Failsafe报告和战斗回放。先看最终Failures/Errors及对应XML，再检查日志；回滚测试中的故障注入不是构建失败的充分证据。调度异常测试使用局部错误收集器断言异常与后续恢复，不隐藏生产错误日志。
