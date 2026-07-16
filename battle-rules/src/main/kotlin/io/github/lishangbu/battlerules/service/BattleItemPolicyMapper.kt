package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleStat
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
		"berry-marker" -> BattleItemEffect.BerryMarker()
		"leftovers-heal" -> BattleItemEffect.HeldEndTurnHeal(healDenominator = 16)
		"life-orb-boost-and-recoil" -> BattleItemEffect.DamageBoostWithRecoil(
			multiplier = 1.3,
			recoilDenominator = 10,
		)
		"held-end-turn-damage-eighth" -> BattleItemEffect.HeldEndTurnDamage(damageDenominator = 8)
		"held-end-turn-major-status-burn" -> BattleItemEffect.HeldEndTurnMajorStatus(BattleMajorStatus.BURN)
		"held-end-turn-major-status-bad-poison" ->
			BattleItemEffect.HeldEndTurnMajorStatus(BattleMajorStatus.BAD_POISON)
		"held-end-turn-heal-poison-sixteenth" -> BattleItemEffect.HeldEndTurnHealForElement(
			elementId = elementIds.requiredElementId("poison"),
			healDenominator = 16,
		)
		"held-end-turn-damage-non-poison-eighth" -> BattleItemEffect.HeldEndTurnDamageWithoutElement(
			elementId = elementIds.requiredElementId("poison"),
			damageDenominator = 8,
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
		"damage-class-power-boost-physical-50" -> BattleItemEffect.DamageClassPowerBoost(
			damageClasses = setOf(BattleDamageClass.PHYSICAL),
			multiplier = 1.5,
		)
		"damage-class-power-boost-special-50" -> BattleItemEffect.DamageClassPowerBoost(
			damageClasses = setOf(BattleDamageClass.SPECIAL),
			multiplier = 1.5,
		)
		"super-effective-damage-boost" -> BattleItemEffect.SuperEffectiveDamageBoost(multiplier = 1.2)
		"small-berry-heal" -> BattleItemEffect.LowHpHeal(fixedHealAmount = 10)
		"medium-berry-heal" -> BattleItemEffect.LowHpHeal(healDenominator = 4)
		"quarter-hp-third-heal-confuse-attack" -> flavorBerryEffect(BattleStat.ATTACK)
		"quarter-hp-third-heal-confuse-defense" -> flavorBerryEffect(BattleStat.DEFENSE)
		"quarter-hp-third-heal-confuse-special-attack" -> flavorBerryEffect(BattleStat.SPECIAL_ATTACK)
		"quarter-hp-third-heal-confuse-special-defense" -> flavorBerryEffect(BattleStat.SPECIAL_DEFENSE)
		"quarter-hp-third-heal-confuse-speed" -> flavorBerryEffect(BattleStat.SPEED)
		"choice-skill-lock" -> BattleItemEffect.ChoiceSkillLock(speedMultiplier = 1.0)
		"choice-speed-lock" -> BattleItemEffect.ChoiceSkillLock(speedMultiplier = 1.5)
		"critical-hit-stage-plus-one" -> BattleItemEffect.CriticalHitStageBoost(stageDelta = 1)
		"low-hp-attack-stage-plus-one" -> BattleItemEffect.LowHpStatStageBoost(BattleStat.ATTACK, stageDelta = 1)
		"low-hp-defense-stage-plus-one" -> BattleItemEffect.LowHpStatStageBoost(BattleStat.DEFENSE, stageDelta = 1)
		"low-hp-speed-stage-plus-one" -> BattleItemEffect.LowHpStatStageBoost(BattleStat.SPEED, stageDelta = 1)
		"low-hp-special-attack-stage-plus-one" ->
			BattleItemEffect.LowHpStatStageBoost(BattleStat.SPECIAL_ATTACK, stageDelta = 1)
		"low-hp-special-defense-stage-plus-one" ->
			BattleItemEffect.LowHpStatStageBoost(BattleStat.SPECIAL_DEFENSE, stageDelta = 1)
		"low-hp-critical-hit-stage-plus-two" -> BattleItemEffect.LowHpCriticalHitStageBoost(stageBonus = 2)
		"low-hp-random-battle-stat-stage-plus-two" -> BattleItemEffect.LowHpRandomStatStageBoost(
			setOf(BattleStat.ATTACK, BattleStat.DEFENSE, BattleStat.SPECIAL_ATTACK, BattleStat.SPECIAL_DEFENSE, BattleStat.SPEED),
			2,
		)
		"low-hp-next-skill-accuracy-six-fifths" -> BattleItemEffect.LowHpNextSkillAccuracyBoost(1.2)
		"special-defense-stat-one-and-half" -> BattleItemEffect.DefendingStatMultiplier(
			stats = setOf(BattleStat.SPECIAL_DEFENSE),
			multiplier = 1.5,
		)
		"status-skill-selection-restriction" -> BattleItemEffect.StatusSkillRestriction()
		"terrain-electric-defense-stage-plus-one" ->
			BattleItemEffect.TerrainActivatedStatStageBoost(BattleTerrain.ELECTRIC, BattleStat.DEFENSE, 1)
		"terrain-grassy-defense-stage-plus-one" ->
			BattleItemEffect.TerrainActivatedStatStageBoost(BattleTerrain.GRASSY, BattleStat.DEFENSE, 1)
		"terrain-misty-special-defense-stage-plus-one" ->
			BattleItemEffect.TerrainActivatedStatStageBoost(BattleTerrain.MISTY, BattleStat.SPECIAL_DEFENSE, 1)
		"terrain-psychic-special-defense-stage-plus-one" ->
			BattleItemEffect.TerrainActivatedStatStageBoost(BattleTerrain.PSYCHIC, BattleStat.SPECIAL_DEFENSE, 1)
		"field-speed-order-trick-room-speed-stage-minus-one" ->
			BattleItemEffect.FieldSpeedOrderActivatedStatStageChange(
				BattleFieldSpeedOrderKind.TRICK_ROOM,
				BattleStat.SPEED,
				-1,
			)
		"received-water-special-attack-stage-plus-one" -> BattleItemEffect.ReceivedDamageStatStageBoost(
			elementId = elementIds.requiredElementId("water"),
			stageChanges = mapOf(BattleStat.SPECIAL_ATTACK to 1),
		)
		"received-electric-attack-stage-plus-one" -> BattleItemEffect.ReceivedDamageStatStageBoost(
			elementId = elementIds.requiredElementId("electric"),
			stageChanges = mapOf(BattleStat.ATTACK to 1),
		)
		"received-water-special-defense-stage-plus-one" -> BattleItemEffect.ReceivedDamageStatStageBoost(
			elementId = elementIds.requiredElementId("water"),
			stageChanges = mapOf(BattleStat.SPECIAL_DEFENSE to 1),
		)
		"received-ice-attack-stage-plus-one" -> BattleItemEffect.ReceivedDamageStatStageBoost(
			elementId = elementIds.requiredElementId("ice"),
			stageChanges = mapOf(BattleStat.ATTACK to 1),
		)
		"received-super-effective-attack-special-attack-stage-plus-two" ->
			BattleItemEffect.ReceivedDamageStatStageBoost(
				requiresSuperEffective = true,
				stageChanges = mapOf(BattleStat.ATTACK to 2, BattleStat.SPECIAL_ATTACK to 2),
			)
		"successful-sound-skill-special-attack-stage-plus-one" ->
			BattleItemEffect.SuccessfulSkillStatStageBoost(
				requiresSoundBased = true,
				stat = BattleStat.SPECIAL_ATTACK,
				stageDelta = 1,
			)
		"accuracy-miss-speed-stage-plus-two" ->
			BattleItemEffect.AccuracyMissStatStageBoost(BattleStat.SPEED, stageDelta = 2)
		"additional-flinch-chance-ten-percent" -> BattleItemEffect.AdditionalFlinchChance(chancePercent = 10)
		"drain-healing-multiplier-thirteen-tenths" -> BattleItemEffect.DrainHealingMultiplier(13, 10)
		"binding-duration-seven" -> BattleItemEffect.BindingDurationOverride(7)
		"binding-damage-denominator-six" -> BattleItemEffect.BindingDamageDenominator(6)
		"random-action-order-boost-twenty-percent" -> BattleItemEffect.RandomActionOrderBoost(20)
		"forced-last-action-order" -> BattleItemEffect.ForcedLastActionOrder()
		"force-grounded" -> BattleItemEffect.GroundingOverride()
		"airborne-until-damaged" -> BattleItemEffect.AirborneUntilDamaged()
		"speed-multiplier-half" -> BattleItemEffect.SpeedMultiplier(0.5)
		"type-immunity-suppression" -> BattleItemEffect.TypeImmunitySuppression()
		"opponent-stat-stage-reduction-immunity" -> BattleItemEffect.OpponentStatStageReductionImmunity()
		"negative-stat-stage-reset" -> BattleItemEffect.NegativeStatStageReset()
		"damaging-skill-secondary-effect-immunity" -> BattleItemEffect.DamagingSkillSecondaryEffectImmunity()
		"consecutive-skill-damage-boost-twenty-percent" -> BattleItemEffect.ConsecutiveSkillDamageBoost()
		"evolvable-defense-special-defense-one-and-half" -> BattleItemEffect.EvolvableDefendingStatMultiplier(
			setOf(BattleStat.DEFENSE, BattleStat.SPECIAL_DEFENSE),
			1.5,
		)
		"ability-ignore-protection" -> BattleItemEffect.AbilityIgnoreProtection()
		"sun-rain-effect-immunity" -> BattleItemEffect.SunRainEffectImmunity()
		"ability-stat-reduction-speed-stage-plus-one" ->
			BattleItemEffect.AbilityStatReductionReactiveBoost(BattleStat.SPEED, 1)
		"opponent-positive-stat-stage-copy" -> BattleItemEffect.OpponentPositiveStatStageCopy()
		"damaged-force-self-switch" -> BattleItemEffect.DamagedForceSelfSwitch()
		"damaged-force-attacker-switch" -> BattleItemEffect.DamagedForceAttackerSwitch()
		"negative-stat-stage-force-self-switch" -> BattleItemEffect.NegativeStatStageForceSelfSwitch()
		"volatile-status-cure-mental-herb" -> BattleItemEffect.VolatileStatusCure(
			setOf(
				BattleVolatileStatus.HEAL_BLOCK,
				BattleVolatileStatus.TAUNT,
				BattleVolatileStatus.DISABLE,
				BattleVolatileStatus.TORMENT,
				BattleVolatileStatus.INFATUATION,
			),
		)
		"infatuation-reflect-to-source" -> BattleItemEffect.InfatuationReflectToSource()
		"highest-stat-booster-abilities-protosynthesis-quark-drive" ->
			BattleItemEffect.HighestStatBoosterActivation(setOf(281, 282))
		"creature-form-override-zacian-crowned" -> BattleItemEffect.CreatureFormOverride(888, 10188)
		"creature-form-override-zamazenta-crowned" -> BattleItemEffect.CreatureFormOverride(889, 10189)
		"low-hp-action-order-boost-quarter" -> BattleItemEffect.LowHpActionOrderBoost()
		"random-fatal-damage-survival-ten-percent" -> BattleItemEffect.RandomFatalDamageSurvival(10)
		"species-pikachu-attack-special-attack-double" -> BattleItemEffect.CreatureStatMultiplier(
			setOf(25), setOf(BattleStat.ATTACK, BattleStat.SPECIAL_ATTACK), 2.0,
		)
		"species-clamperl-special-attack-double" ->
			BattleItemEffect.CreatureStatMultiplier(setOf(366), setOf(BattleStat.SPECIAL_ATTACK), 2.0)
		"species-clamperl-special-defense-double" ->
			BattleItemEffect.CreatureStatMultiplier(setOf(366), setOf(BattleStat.SPECIAL_DEFENSE), 2.0)
		"species-dialga-steel-dragon-power-one-fifth" -> BattleItemEffect.CreatureElementDamageBoost(
			setOf(483), setOf(elementIds.requiredElementId("steel"), elementIds.requiredElementId("dragon")), 1.2,
		)
		"species-palkia-water-dragon-power-one-fifth" -> BattleItemEffect.CreatureElementDamageBoost(
			setOf(484), setOf(elementIds.requiredElementId("water"), elementIds.requiredElementId("dragon")), 1.2,
		)
		"species-giratina-ghost-dragon-power-one-fifth" -> BattleItemEffect.CreatureElementDamageBoost(
			setOf(487, 10007), setOf(elementIds.requiredElementId("ghost"), elementIds.requiredElementId("dragon")), 1.2,
		)
		"species-latias-latios-psychic-dragon-power-one-fifth" -> BattleItemEffect.CreatureElementDamageBoost(
			setOf(380, 381), setOf(elementIds.requiredElementId("psychic"), elementIds.requiredElementId("dragon")), 1.2,
		)
		"species-ogerpon-wellspring-all-power-one-fifth" -> BattleItemEffect.CreatureDamageBoost(setOf(10273), 1.2)
		"species-ogerpon-hearthflame-all-power-one-fifth" -> BattleItemEffect.CreatureDamageBoost(setOf(10274), 1.2)
		"species-ogerpon-cornerstone-all-power-one-fifth" -> BattleItemEffect.CreatureDamageBoost(setOf(10275), 1.2)
		"consumable-element-damage-boost-normal-thirty-percent" ->
			BattleItemEffect.ConsumableElementDamageBoost(elementIds.requiredElementId("normal"), 1.3)
		"accuracy-multiplier-eleven-tenths" -> BattleItemEffect.AccuracyMultiplier(multiplier = 1.1)
		"accuracy-multiplier-six-fifths-after-target-acted" ->
			BattleItemEffect.AccuracyMultiplierAfterTargetActed(multiplier = 1.2)
		"opponent-accuracy-multiplier-nine-tenths" ->
			BattleItemEffect.OpponentAccuracyMultiplier(multiplier = 0.9)
		"powder-skill-immunity" -> BattleItemEffect.PowderSkillImmunity()
		"entry-hazard-immunity" -> BattleItemEffect.EntryHazardImmunity()
		"switch-restriction-immunity" -> BattleItemEffect.SwitchRestrictionImmunity()
		"standard-multi-hit-count-four-to-five" ->
			BattleItemEffect.MultiHitCountRangeOverride(minHits = 4, maxHits = 5)
		"weather-damage-immunity-sandstorm" -> BattleItemEffect.WeatherDamageImmunity(
			weathers = setOf(BattleWeather.SANDSTORM),
		)
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
		"contact-transfer-to-attacker" -> BattleItemEffect.ContactTransferToAttacker()
		"punch-based-skill-power-boost" -> BattleItemEffect.PunchBasedSkillPowerBoost()
		"punch-based-contact-suppression" -> BattleItemEffect.PunchBasedContactSuppression()
		"contact-side-effect-immunity" -> BattleItemEffect.ContactSideEffectImmunity()
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

private fun flavorBerryEffect(dislikedStat: BattleStat): BattleItemEffect.LowHpHeal =
	BattleItemEffect.LowHpHeal(
		triggerHpNumerator = 1,
		triggerHpDenominator = 4,
		healDenominator = 3,
		confusesIfNatureDecreases = dislikedStat,
	)
