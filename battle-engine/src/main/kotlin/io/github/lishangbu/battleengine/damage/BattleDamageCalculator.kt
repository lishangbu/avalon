package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.model.BattleDamageClass
import kotlin.math.floor

/**
 * 现代普通伤害公式计算器。
 *
 * 该实现覆盖第一阶段 MVP 需要的基础公式：等级、威力、攻击/防御、随机浮动、属性一致加成和属性克制。
 * 它暂不处理击中要害、灼伤、天气、场地、道具、特性、范围技能、护盾和其它倍率；这些倍率会在后续 hook
 * 管线中以结构化 modifier 追加，并由对照 fixture 分别验证。
 *
 * 取整规则按主系列常见公开公式建模：基础伤害部分在整数除法中逐步截断，最终倍率组合后向下取整。
 * 如果属性克制倍率为 0，最终伤害为 0；否则普通命中造成的最小伤害为 1。
 */
class BattleDamageCalculator {
	/**
	 * 计算一次物理或特殊技能的普通伤害。
	 *
	 * @throws IllegalArgumentException 当技能为变化技能、缺少威力或防御能力无效时抛出。
	 */
	fun calculate(request: BattleDamageRequest): BattleDamageResult {
		require(request.skill.damageClass != BattleDamageClass.STATUS) { "status skill does not use standard damage formula" }
		val power = requireNotNull(request.skill.power) { "damaging skill must define power" }
		val attackingStat = when (request.skill.damageClass) {
			BattleDamageClass.PHYSICAL -> request.attacker.attack
			BattleDamageClass.SPECIAL -> request.attacker.specialAttack
			BattleDamageClass.STATUS -> error("status skill does not use standard damage formula")
		}
		val defendingStat = when (request.skill.damageClass) {
			BattleDamageClass.PHYSICAL -> request.defender.defense
			BattleDamageClass.SPECIAL -> request.defender.specialDefense
			BattleDamageClass.STATUS -> error("status skill does not use standard damage formula")
		}
		require(defendingStat > 0) { "defending stat must be positive" }

		val levelFactor = (2 * request.attacker.level) / 5 + 2
		val baseDamage = (((levelFactor * power * attackingStat) / defendingStat) / 50) + 2
		val sameElementBonus = if (request.skill.elementId in request.attacker.elementIds) 1.5 else 1.0
		val effectiveness = request.rules.elementChart.multiplier(request.skill.elementId, request.defender.elementIds)
		val combined = baseDamage * (request.randomPercent / 100.0) * sameElementBonus * effectiveness
		val amount = if (effectiveness == 0.0) 0 else floor(combined).toInt().coerceAtLeast(1)
		return BattleDamageResult(
			amount = amount,
			baseDamage = baseDamage,
			sameElementBonus = sameElementBonus,
			effectiveness = effectiveness,
		)
	}
}
