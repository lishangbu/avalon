# 内容事实来源

以下路径相对于 avalon-content/src/main/resources/。

| 内容 | 正式来源 |
| --- | --- |
| 任务规则、步骤、分支、目标、奖励及阶段对白 | content/quests/<区域>/<任务链>.json |
| 任务标签字典 | content/quests/tags.json |
| 地点、道路、NPC与服务 | content/world/opening.json |
| 现场动作 | content/world/scene-actions.json |
| 战斗与掉落 | content/battle/battle.json、content/battle/wilds.json |
| 人物装备、商店 | content/items/items.json、content/items/equipment.json |
| 采集、配方与制造等级 | content/professions/gathering.json、content/professions/life-production.json |
| 学院临时课程 | content/battle/academy-training.json |
| 副本定义、奖励与宝箱 | content/dungeons/<副本>.json |

JSON 为唯一内容维护来源；任务链与副本自动发现。审计脚本只读取配置，任务目录导出脚本只更新文档。
运行时、页面与测试共用同一正式目录，不维护旧路径映射、剧情编号转换或第二套奖励表。
