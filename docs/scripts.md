# 当前脚本入口

脚本从仓库根目录执行；正式数值以content目录为准，脚本不另行维护游戏配置。生成报告写入忽略的outputs目录，不作为长期文档提交。

| 入口 | 用途与边界 |
| --- | --- |
| `node scripts/export-quest-catalog.mjs` | 从正式任务链更新任务目录文档 |
| `node scripts/audit-story-objectives.mjs --markdown docs/quest-objective-audit.md --check` | 检查任务目标分类文档与配置一致 |
| `node scripts/audit-story-growth.mjs` | 任务经验、对白经验数字及分段巡逻预算，不证明完整旅程 |
| `bash scripts/validate-quests.sh` | 构建依赖并运行正式任务目录校验，需要JDK与Maven环境 |
| `python scripts/check-java-imports.py` | 检查公开类型组织、正文全限定类型与重复导入 |
| `node scripts/test-manufacturing.mjs integration` | 制造相关正式服务专项，需要Docker |
| `node scripts/simulate-postgame.mjs` | 统一运行模型测试和四类离线报告 |
| `node scripts/audit-postgame-loop.mjs` | 试炼和制造材料来源预算 |
| `node scripts/simulate-postgame-professions.mjs` | 专业直接练级预算与稀有采集样本 |
| `node scripts/simulate-postgame-supply.mjs` | 成熟供料网络的工具、材料及交易税场景 |
| `node scripts/simulate-production-journey.mjs` | 顺序制造、采集、库存、现金流和道路的条件模型 |
| `node scripts/simulate-gathering-recovery.mjs` | 指定伙伴资源容量下的采集恢复场景，参数见脚本 |
| `python scripts/dungeon-reward-comparison.py --help` | 比较当前浏览器生成的副本与野战计时证据，需要先生成证据 |
| `bash scripts/backup-postgres.sh` | 按脚本参数备份PostgreSQL，部署方式见Docker文档 |

`content-files.mjs` 为内容读取公共模块；`growth-model.mjs`、`production-cost-model.mjs`、`production-ledger.mjs` 是模型库，相应 `.test.mjs` 通过 `node --test` 执行。它们由真实入口使用，不能按没有命令行输出判为无用文件。

离线模型不执行完整剧情与战斗。全图开放、指定伙伴容量、已有供料角色等是假设，需要与正式单人路线区分；不得将条件模型结果累加成新号真实游戏时间。
