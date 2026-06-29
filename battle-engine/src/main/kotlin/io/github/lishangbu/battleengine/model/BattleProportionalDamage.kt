package io.github.lishangbu.battleengine.model

/**
 * 技能命中后按目标当前 HP 比例造成直接伤害的规则。
 *
 * 比例伤害和固定伤害一样属于“命中后的直接技能伤害”，但它的基础数值来自目标当前 HP，而不是技能威力、
 * 使用者等级或普通伤害公式。它不会消费击中要害或伤害浮动随机数，也不会读取属性一致加成、属性克制倍率、
 * 攻防能力值、天气、场地、道具或特性伤害倍率。
 *
 * 目标仍然需要先通过保护、命中、属性免疫、特性吸收和替身等前置流程；属性相性为 0 时不会造成 HP 变化。
 * 反击、最终搏命、让目标 HP 变为使用者 HP、随机固定伤害等差异较大的规则会用后续专门模型表达。
 */
sealed interface BattleProportionalDamage {
	/**
	 * 按目标当前 HP 的固定比例造成伤害。
	 *
	 * 该规则读取命中时目标的当前 HP，并按 `numerator / denominator` 向下取整；只要目标当前 HP 大于 0，结果
	 * 至少为 [minimumDamage]。状态机随后再按目标或替身的剩余 HP 夹取实际损失，因此不会产生负 HP 或溢出伤害。
	 */
	data class TargetCurrentHpFraction(
		val numerator: Int,
		val denominator: Int,
		val minimumDamage: Int = 1,
	) : BattleProportionalDamage {
		init {
			require(numerator > 0) { "numerator must be positive" }
			require(denominator > 0) { "denominator must be positive" }
			require(numerator <= denominator) { "numerator must not exceed denominator" }
			require(minimumDamage > 0) { "minimumDamage must be positive" }
		}
	}
}
