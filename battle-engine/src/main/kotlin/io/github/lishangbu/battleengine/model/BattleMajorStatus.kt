package io.github.lishangbu.battleengine.model

/**
 * 主要异常状态。
 *
 * 枚举只表达会进入核心战斗结算的稳定状态事实，不保存来源、持续文本或外部资料 ID。具体行为分散在对应处理器：
 * 灼伤降低物理伤害并在回合末扣血，麻痹降低速度并在行动前按固定概率阻止技能，中毒和剧毒在回合末扣血，
 * 睡眠按剩余行动阻止次数拦截技能，冰冻按行动前自然解冻概率决定是否继续阻止技能。技能、道具或其它效果带来的
 * 解除逻辑会通过状态处理器修改成员快照，而不是扩展新的枚举值。
 */
enum class BattleMajorStatus {
	BURN,
	PARALYSIS,
	POISON,
	BAD_POISON,
	SLEEP,
	FREEZE,
}
