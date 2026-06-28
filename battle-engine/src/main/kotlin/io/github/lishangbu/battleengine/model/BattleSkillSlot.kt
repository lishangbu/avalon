package io.github.lishangbu.battleengine.model

/**
 * 战斗成员在本场战斗中可使用的一格技能。
 *
 * 该对象是战斗初始状态的一部分，已经合并了基础技能资料和规则快照所需的最小执行字段：
 * 属性、伤害类别、威力、命中、击中要害等级、保护交互、优先度和 PP。它不保存技能文本说明，
 * 也不保存数据库实体引用对象。
 *
 * `power` 和 `accuracy` 允许为空，用于表达变化技能、必中技能或由特殊策略决定数值的技能。
 * `targetScope` 表示技能在站位中的目标范围，供双打范围技能计算实际目标和 0.75 伤害修正。
 * `criticalHitStage` 表示进入现代击中要害概率表前的技能侧基础等级，0 为普通技能，3 及以上视为必定要害。
 * `protectsUser` 表示该技能在本回合为使用者建立保护屏障；`affectedByProtect` 表示该技能命中目标时会被
 * 目标的保护屏障阻挡。两者拆开建模，是为了后续支持佯攻、Z 类强化效果、范围技能和穿透保护的特殊技能。
 *
 * 第一阶段普通伤害公式只处理带威力的物理/特殊技能；特殊技能效果会继续通过显式规则对象扩展。
 */
data class BattleSkillSlot(
	val skillId: Long,
	val name: String,
	val elementId: Long,
	val damageClass: BattleDamageClass,
	val power: Int?,
	val accuracy: Int?,
	val targetScope: BattleSkillTargetScope = BattleSkillTargetScope.SELECTED_TARGET,
	val makesContact: Boolean = false,
	val criticalHitStage: Int = 0,
	val affectedByProtect: Boolean = true,
	val protectsUser: Boolean = false,
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
		require(criticalHitStage >= 0) { "criticalHitStage must not be negative" }
		require(!protectsUser || damageClass == BattleDamageClass.STATUS) { "protect skill must be a status skill" }
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
