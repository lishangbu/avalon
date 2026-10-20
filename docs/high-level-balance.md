# 后期成长与战斗预算

成长阈值以 content/rules/experience.json 为准；遭遇、物种成长、经验与金币倍率分别由 content/battle/battle.json、content/battle/wilds.json 和 content/creatures/species-stats.json 定义。

主线后期由玩家通过正式刷怪、副本和可选任务继续成长。巡逻倍率及估算入口见[当前实施状态](implementation-progress.md)。

运行 scripts/audit-story-growth.mjs 可复算任务经验与额外战斗场数区间。该脚本只提供预算，不模拟完整战斗胜率、补给成本、战败、旅行、装备更新或真人操作时长。
经验预算必须区分人物等级、伙伴等级和专业等级，不能把高经验倍率视为装备、专业与补给同步完成。

验收应使用真实职业、人物装备、伙伴编队和消耗，分别记录任务战斗、额外巡逻、恢复、装备更换和副本结算；失败与重试同样计入投入。
长期经济、制造投入和真人节奏的剩余验证范围见[工程验收](engineering-acceptance.md)。
