package io.github.lishangbu.battleengine.model

/**
 * 战斗成员在本场战斗中可使用的一格技能。
 *
 * 该对象是战斗初始状态的一部分，已经合并了基础技能资料和规则快照所需的最小执行字段：
 * 属性、伤害类别、威力、命中、优先度和 PP。它不保存技能文本说明，也不保存数据库实体引用对象。
 *
 * `power` 和 `accuracy` 允许为空，用于表达变化技能、必中技能或由特殊策略决定数值的技能。
 * 第一阶段普通伤害公式只处理带威力的物理/特殊技能。
 */
data class BattleSkillSlot(
	val skillId: Long,
	val name: String,
	val elementId: Long,
	val damageClass: BattleDamageClass,
	val power: Int?,
	val accuracy: Int?,
	val makesContact: Boolean = false,
	val priority: Int = 0,
	val remainingPp: Int,
	val maxPp: Int,
	val statusApplications: List<BattleStatusApplication> = emptyList(),
	val statStageEffects: List<BattleStatStageEffect> = emptyList(),
) {
	init {
		require(skillId > 0) { "skillId must be positive" }
		require(name.isNotBlank()) { "skill name must not be blank" }
		require(elementId > 0) { "elementId must be positive" }
		require(power == null || power > 0) { "power must be positive when present" }
		require(accuracy == null || accuracy in 1..100) { "accuracy must be between 1 and 100 when present" }
		require(remainingPp in 0..maxPp) { "remainingPp must be between 0 and maxPp" }
		require(maxPp >= 0) { "maxPp must not be negative" }
	}

	/**
	 * 返回消耗 1 点 PP 后的技能槽。
	 *
	 * 调用方必须先检查 `remainingPp`，这里仍保留 require 作为状态机不变量保护。
	 */
	fun consumePp(): BattleSkillSlot {
		require(remainingPp > 0) { "skill has no remaining PP" }
		return copy(remainingPp = remainingPp - 1)
	}
}
