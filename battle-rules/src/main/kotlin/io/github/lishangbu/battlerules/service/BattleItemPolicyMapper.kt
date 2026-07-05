package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleWeather

/**
 * 道具运行时 policy 映射器。
 *
 * `battle_item_rule.effect_policy` 只负责把携带道具的稳定资料 code 翻译成 [BattleItemEffect]。属性增强和属性减伤这类
 * 同构 policy 使用前缀解析，普通道具效果保留明确分支；这样既能减少重复，又能让一次性保命、状态解除、天气/场地
 * 延长等规则保持逐项可读。
 */
internal fun String.toBattleItemEffect(elementIds: Map<String, Long>): BattleItemEffect? =
	when (this) {
		"leftovers-heal" -> BattleItemEffect.HeldEndTurnHeal(healDenominator = 16)
		"life-orb-boost-and-recoil" -> BattleItemEffect.DamageBoostWithRecoil(
			multiplier = 1.3,
			recoilDenominator = 10,
		)
		"damage-dealt-heal-eighth" -> BattleItemEffect.DamageDealtHeal(healDenominator = 8)
		"damage-class-power-boost-physical" -> BattleItemEffect.DamageClassPowerBoost(
			damageClasses = setOf(BattleDamageClass.PHYSICAL),
			multiplier = 1.1,
		)
		"damage-class-power-boost-special" -> BattleItemEffect.DamageClassPowerBoost(
			damageClasses = setOf(BattleDamageClass.SPECIAL),
			multiplier = 1.1,
		)
		"super-effective-damage-boost" -> BattleItemEffect.SuperEffectiveDamageBoost(multiplier = 1.2)
		"small-berry-heal" -> BattleItemEffect.LowHpHeal(fixedHealAmount = 10)
		"medium-berry-heal" -> BattleItemEffect.LowHpHeal(healDenominator = 4)
		"choice-speed-lock" -> BattleItemEffect.ChoiceSkillLock(speedMultiplier = 1.5)
		"weight-half" -> BattleItemEffect.WeightMultiplier(
			numerator = 1,
			denominator = 2,
		)
		"major-status-cure-paralysis" -> BattleItemEffect.MajorStatusCure(
			statuses = setOf(BattleMajorStatus.PARALYSIS),
		)
		"major-status-cure-sleep" -> BattleItemEffect.MajorStatusCure(
			statuses = setOf(BattleMajorStatus.SLEEP),
		)
		"major-status-cure-poison" -> BattleItemEffect.MajorStatusCure(
			statuses = setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
		)
		"major-status-cure-burn" -> BattleItemEffect.MajorStatusCure(
			statuses = setOf(BattleMajorStatus.BURN),
		)
		"major-status-cure-freeze" -> BattleItemEffect.MajorStatusCure(
			statuses = setOf(BattleMajorStatus.FREEZE),
		)
		"major-status-cure-all" -> BattleItemEffect.MajorStatusCure(
			statuses = setOf(
				BattleMajorStatus.BURN,
				BattleMajorStatus.PARALYSIS,
				BattleMajorStatus.POISON,
				BattleMajorStatus.BAD_POISON,
				BattleMajorStatus.SLEEP,
				BattleMajorStatus.FREEZE,
			),
		)
		"volatile-status-cure-confusion" -> BattleItemEffect.VolatileStatusCure(
			statuses = setOf(BattleVolatileStatus.CONFUSION),
		)
		"contact-damage-to-attacker-sixth" -> BattleItemEffect.ContactDamageToAttacker(damageDenominator = 6)
		"punch-based-skill-power-boost" -> BattleItemEffect.PunchBasedSkillPowerBoost()
		"punch-based-contact-suppression" -> BattleItemEffect.PunchBasedContactSuppression
		"contact-side-effect-immunity" -> BattleItemEffect.ContactSideEffectImmunity
		"charge-skip-once" -> BattleItemEffect.ChargeSkipOnce()
		"consumable-full-hp-fatal-damage-survival" -> BattleItemEffect.SurviveFatalDamageAtFullHp()
		"side-condition-duration-screen" -> BattleItemEffect.SideDamageReductionDurationExtension(
			kinds = setOf(
				BattleSideDamageReductionKind.PHYSICAL,
				BattleSideDamageReductionKind.SPECIAL,
				BattleSideDamageReductionKind.ALL_STANDARD_DAMAGE,
			),
			turnsRemaining = 8,
		)
		"weather-duration-rain" -> BattleItemEffect.WeatherDurationExtension(
			weathers = setOf(BattleWeather.RAIN),
			turnsRemaining = 8,
		)
		"weather-duration-sandstorm" -> BattleItemEffect.WeatherDurationExtension(
			weathers = setOf(BattleWeather.SANDSTORM),
			turnsRemaining = 8,
		)
		"weather-duration-snow" -> BattleItemEffect.WeatherDurationExtension(
			weathers = setOf(BattleWeather.SNOW),
			turnsRemaining = 8,
		)
		"weather-duration-sun" -> BattleItemEffect.WeatherDurationExtension(
			weathers = setOf(BattleWeather.SUN),
			turnsRemaining = 8,
		)
		"terrain-duration-all" -> BattleItemEffect.TerrainDurationExtension(
			terrains = setOf(
				BattleTerrain.ELECTRIC,
				BattleTerrain.GRASSY,
				BattleTerrain.MISTY,
				BattleTerrain.PSYCHIC,
			),
			turnsRemaining = 8,
		)
		else -> toElementDamageBoostItemEffect(elementIds)
			?: toElementDamageReductionItemEffect(elementIds)
	}

private const val ELEMENT_DAMAGE_BOOST_ITEM_POLICY_PREFIX = "element-damage-boost-"
private const val ELEMENT_DAMAGE_REDUCTION_ITEM_POLICY_PREFIX = "element-damage-reduction-"

/**
 * 将“某属性技能威力增强道具”统一映射成运行时效果。
 *
 * 这类 policy 的结构是 `element-damage-boost-{elementCode}`，所有属性都使用相同的 1.2 倍倍率。把属性 code
 * 留在 policy 尾部，比为 18 个属性各写一段 `when` 分支更接近资料本身，也避免后续新增属性资料或修正属性 code
 * 时需要同步修改一长串重复 Kotlin 分支。
 */
private fun String.toElementDamageBoostItemEffect(elementIds: Map<String, Long>): BattleItemEffect.ElementDamageBoost? {
	val elementCode = removePrefix(ELEMENT_DAMAGE_BOOST_ITEM_POLICY_PREFIX)
	if (elementCode == this) {
		return null
	}
	val elementId = elementIds[elementCode] ?: return null
	return BattleItemEffect.ElementDamageBoost(
		elementId = elementId,
		multiplier = 1.2,
	)
}

/**
 * 将“某属性减伤道具”统一映射成运行时效果。
 *
 * 减伤类 policy 的结构是 `element-damage-reduction-{elementCode}`，默认表示对应属性效果绝佳时把伤害减半。
 * 一般属性没有效果绝佳关系，因此 `normal` 是唯一需要关闭 [BattleItemEffect.ElementDamageReduction.requiresSuperEffective]
 * 的例外；这个例外保留在同一个 helper 里，避免散落成 18 个几乎相同的分支。
 */
private fun String.toElementDamageReductionItemEffect(
	elementIds: Map<String, Long>,
): BattleItemEffect.ElementDamageReduction? {
	val elementCode = removePrefix(ELEMENT_DAMAGE_REDUCTION_ITEM_POLICY_PREFIX)
	if (elementCode == this) {
		return null
	}
	val elementId = elementIds[elementCode] ?: return null
	return BattleItemEffect.ElementDamageReduction(
		elementId = elementId,
		multiplier = 0.5,
		requiresSuperEffective = elementCode != "normal",
	)
}

/**
 * 判断道具 `effect_policy` 是否已被运行时效果模型承载。
 *
 * 道具没有类似接地事实的旁路字段；启用中的道具规则如果不能映射成 [BattleItemEffect]，就意味着资料存在但战斗引擎
 * 完全不会执行它，所以完整性测试必须把这种情况作为失败处理。
 */
internal fun String.isBattleItemRuntimePolicySupported(elementIds: Map<String, Long>): Boolean =
	toBattleItemEffect(elementIds) != null
