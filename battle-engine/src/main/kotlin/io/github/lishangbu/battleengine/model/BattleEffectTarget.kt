package io.github.lishangbu.battleengine.model

/**
 * 技能附加效果的作用对象。
 *
 * 这里描述的是“某一次逐目标结算中的效果落点”，不是玩家在行动请求里选择的目标范围。范围技能会先由
 * [BattleSkillTargetScope] 解析成多个实际目标，然后对每个目标分别执行附加效果；因此大多数技能后效只需要
 * `USER` 和 `TARGET` 两个相对位置，就能同时覆盖单体、随机目标和双打范围目标。
 */
enum class BattleEffectTarget {
	USER,
	TARGET,
}
