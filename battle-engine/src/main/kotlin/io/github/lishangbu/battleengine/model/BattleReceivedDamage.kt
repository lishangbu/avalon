package io.github.lishangbu.battleengine.model

/**
 * 技能按使用者本回合已经承受的直接伤害反打目标。
 *
 * 这类技能没有基础威力，也不进入普通物理/特殊伤害公式；它们读取“本回合最后一次由对手造成、且符合伤害类别
 * 条件的 HP 伤害”，再按固定倍数把伤害返还给该伤害来源。把规则做成显式模型有两个目的：
 * - 引擎不需要按技能 ID、名称或本地化文本判断特殊技能，后续资料修正只改运行态装配。
 * - 事件流仍是唯一事实来源；replay 复算时只要前面的 [BattleEvent.DamageApplied] 顺序一致，反打目标和伤害
 *   就会自然一致，不需要额外持久化“上一段伤害缓存”。
 *
 * `acceptedDamageClasses` 只允许物理或特殊伤害，间接伤害、异常伤害、天气伤害和变化技能不会成为反打来源。
 * `ignoreNonImmuneElementEffectiveness` 表示属性相性只用于判断完全免疫；只要倍率不是 0，反打伤害数值就按记忆伤害
 * 乘数结算，不再套用克制或抵抗倍率。
 */
data class BattleReceivedDamage(
	val acceptedDamageClasses: Set<BattleDamageClass>,
	val numerator: Int,
	val denominator: Int,
	val ignoreNonImmuneElementEffectiveness: Boolean = true,
) {
	init {
		require(acceptedDamageClasses.isNotEmpty()) { "acceptedDamageClasses must not be empty" }
		require(BattleDamageClass.STATUS !in acceptedDamageClasses) {
			"received damage skills can only remember physical or special damage"
		}
		require(numerator > 0) { "numerator must be positive" }
		require(denominator > 0) { "denominator must be positive" }
	}
}
