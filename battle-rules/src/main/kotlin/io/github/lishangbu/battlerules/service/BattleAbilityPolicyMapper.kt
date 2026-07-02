package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather

/**
 * 特性运行时 policy 映射器。
 *
 * `battle_ability_rule.effect_policy` 既有直接映射成 [BattleAbilityEffect] 的效果，也有 `ground-immunity` 这种写入成员
 * 接地事实的旁路 policy。本文件把这些特性专属规则从通用 code mapper 中拆出，避免技能和道具新增 policy 时反复
 * 触碰同一个大文件。
 */
internal fun String.toBattleAbilityEffect(elementIds: Map<String, Long>): BattleAbilityEffect? =
	when (this) {
		"low-hp-grass-boost" -> BattleAbilityEffect.LowHpElementDamageBoost(
			elementId = elementIds.requiredElementId("grass"),
		)
		"low-hp-fire-boost" -> BattleAbilityEffect.LowHpElementDamageBoost(
			elementId = elementIds.requiredElementId("fire"),
		)
		"low-hp-water-boost" -> BattleAbilityEffect.LowHpElementDamageBoost(
			elementId = elementIds.requiredElementId("water"),
		)
		"low-hp-bug-boost" -> BattleAbilityEffect.LowHpElementDamageBoost(
			elementId = elementIds.requiredElementId("bug"),
		)
		"element-dragon-damage-boost" -> BattleAbilityEffect.ElementSkillDamageBoost(
			elementIds = setOf(elementIds.requiredElementId("dragon")),
			multiplier = 1.5,
		)
		"element-rock-damage-boost" -> BattleAbilityEffect.ElementSkillDamageBoost(
			elementIds = setOf(elementIds.requiredElementId("rock")),
			multiplier = 1.5,
		)
		"element-steel-damage-boost" -> BattleAbilityEffect.ElementSkillDamageBoost(
			elementIds = setOf(elementIds.requiredElementId("steel")),
			multiplier = 1.5,
		)
		"element-electric-damage-boost" -> BattleAbilityEffect.ElementSkillDamageBoost(
			elementIds = setOf(elementIds.requiredElementId("electric")),
			multiplier = 1.3,
		)
		"weather-sandstorm-rock-ground-steel-damage-boost" -> BattleAbilityEffect.WeatherElementDamageBoost(
			weather = BattleWeather.SANDSTORM,
			elementIds = setOf(
				elementIds.requiredElementId("rock"),
				elementIds.requiredElementId("ground"),
				elementIds.requiredElementId("steel"),
			),
		)
		"punch-based-skill-damage-boost" -> BattleAbilityEffect.PunchBasedSkillDamageBoost()
		"slicing-based-skill-damage-boost" -> BattleAbilityEffect.SlicingBasedSkillDamageBoost()
		"contact-based-skill-damage-boost" -> BattleAbilityEffect.ContactBasedSkillDamageBoost()
		"sound-based-skill-damage-boost" -> BattleAbilityEffect.SoundBasedSkillDamageBoost()
		"sound-based-skill-damage-reduction" -> BattleAbilityEffect.SoundBasedSkillDamageReduction()
		"super-effective-damage-reduction" -> BattleAbilityEffect.SuperEffectiveDamageReduction()
		"full-hp-damage-reduction" -> BattleAbilityEffect.FullHpDamageReduction()
		"special-damage-reduction" -> BattleAbilityEffect.DamageClassDamageReduction(
			damageClasses = setOf(BattleDamageClass.SPECIAL),
		)
		"defense-stat-double" -> BattleAbilityEffect.DefendingStatMultiplier(
			stat = BattleStat.DEFENSE,
			multiplier = 2.0,
		)
		"grassy-terrain-defense-stat-boost" -> BattleAbilityEffect.DefendingStatMultiplier(
			stat = BattleStat.DEFENSE,
			multiplier = 1.5,
			requiredTerrain = BattleTerrain.GRASSY,
		)
		"attack-stat-double" -> BattleAbilityEffect.AttackingStatMultiplier(
			stat = BattleStat.ATTACK,
			multiplier = 2.0,
		)
		"major-status-attack-stat-boost-ignore-burn-drop" -> BattleAbilityEffect.AttackingStatMultiplier(
			stat = BattleStat.ATTACK,
			multiplier = 1.5,
			requiresMajorStatus = true,
			ignoresBurnAttackReduction = true,
		)
		"same-element-bonus-double" -> BattleAbilityEffect.SameElementBonusOverride(
			multiplier = 2.0,
		)
		// 现代接触反制类特性按 30% 附加主要异常状态；当前种子里只有麻痹变体。
		"contact-paralysis" -> BattleAbilityEffect.ContactStatusOnAttacker(
			status = BattleMajorStatus.PARALYSIS,
			chancePercent = 30,
		)
		"switch-in-opponents-attack-down" -> BattleAbilityEffect.SwitchInStatStageChange(
			stat = BattleStat.ATTACK,
			stageDelta = -1,
		)
		"switch-in-weather-rain" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.RAIN)
		"switch-in-weather-sandstorm" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.SANDSTORM)
		"switch-in-weather-snow" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.SNOW)
		"switch-in-weather-sun" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.SUN)
		"switch-in-terrain-electric" -> BattleAbilityEffect.SwitchInTerrainChange(BattleTerrain.ELECTRIC)
		"switch-in-terrain-grassy" -> BattleAbilityEffect.SwitchInTerrainChange(BattleTerrain.GRASSY)
		"switch-in-terrain-misty" -> BattleAbilityEffect.SwitchInTerrainChange(BattleTerrain.MISTY)
		"switch-in-terrain-psychic" -> BattleAbilityEffect.SwitchInTerrainChange(BattleTerrain.PSYCHIC)
		"weather-speed-rain" -> BattleAbilityEffect.WeatherSpeedMultiplier(
			weather = BattleWeather.RAIN,
			multiplier = 2.0,
		)
		"weather-speed-sandstorm" -> BattleAbilityEffect.WeatherSpeedMultiplier(
			weather = BattleWeather.SANDSTORM,
			multiplier = 2.0,
		)
		"weather-speed-snow" -> BattleAbilityEffect.WeatherSpeedMultiplier(
			weather = BattleWeather.SNOW,
			multiplier = 2.0,
		)
		"weather-speed-sun" -> BattleAbilityEffect.WeatherSpeedMultiplier(
			weather = BattleWeather.SUN,
			multiplier = 2.0,
		)
		"terrain-speed-electric" -> BattleAbilityEffect.TerrainSpeedMultiplier(
			terrain = BattleTerrain.ELECTRIC,
			multiplier = 2.0,
		)
		"weight-double" -> BattleAbilityEffect.WeightMultiplier(
			numerator = 2,
			denominator = 1,
		)
		"weight-half" -> BattleAbilityEffect.WeightMultiplier(
			numerator = 1,
			denominator = 2,
		)
		"weather-heal-rain" -> BattleAbilityEffect.WeatherEndTurnHeal(
			weathers = setOf(BattleWeather.RAIN),
			healDenominator = 16,
		)
		"weather-heal-snow" -> BattleAbilityEffect.WeatherEndTurnHeal(
			weathers = setOf(BattleWeather.SNOW),
			healDenominator = 16,
		)
		"critical-hit-immunity" -> BattleAbilityEffect.CriticalHitImmunity
		"full-hp-fatal-damage-survival" -> BattleAbilityEffect.SurviveFatalDamageAtFullHp()
		"indirect-damage-immunity" -> BattleAbilityEffect.IndirectDamageImmunity
		"weather-damage-immunity-sandstorm" -> BattleAbilityEffect.WeatherDamageImmunity(
			weathers = setOf(BattleWeather.SANDSTORM),
		)
		"skill-recoil-damage-immunity" -> BattleAbilityEffect.SkillRecoilDamageImmunity
		"ignore-opponent-accuracy-stat-stages" -> BattleAbilityEffect.IgnoreOpponentAccuracyStatStages
		"ignore-opponent-damage-stat-stages" -> BattleAbilityEffect.IgnoreOpponentDamageStatStages
		"ignore-target-ability-effects" -> BattleAbilityEffect.IgnoreTargetAbilityEffects
		"sound-based-skill-immunity" -> BattleAbilityEffect.SoundBasedSkillImmunity
		"side-priority-move-immunity" -> BattleAbilityEffect.PriorityMoveImmunityForSide()
		"status-skill-priority-boost" -> BattleAbilityEffect.StatusSkillPriorityBoost()
		"element-electric-absorb-heal" -> BattleAbilityEffect.ElementSkillAbsorbHeal(
			elementId = elementIds.requiredElementId("electric"),
		)
		"element-water-absorb-heal" -> BattleAbilityEffect.ElementSkillAbsorbHeal(
			elementId = elementIds.requiredElementId("water"),
		)
		"element-ground-absorb-heal" -> BattleAbilityEffect.ElementSkillAbsorbHeal(
			elementId = elementIds.requiredElementId("ground"),
		)
		"element-electric-absorb-speed-up" -> BattleAbilityEffect.ElementSkillAbsorbStatStage(
			elementId = elementIds.requiredElementId("electric"),
			stat = BattleStat.SPEED,
			stageDelta = 1,
		)
		"element-grass-absorb-attack-up" -> BattleAbilityEffect.ElementSkillAbsorbStatStage(
			elementId = elementIds.requiredElementId("grass"),
			stat = BattleStat.ATTACK,
			stageDelta = 1,
		)
		"element-fire-absorb-defense-up-two" -> BattleAbilityEffect.ElementSkillAbsorbStatStage(
			elementId = elementIds.requiredElementId("fire"),
			stat = BattleStat.DEFENSE,
			stageDelta = 2,
		)
		// 接地免疫会写入 BattleParticipant.grounded，不作为独立效果返回。
		"ground-immunity" -> null
		else -> null
	}

/**
 * 判断特性 `effect_policy` 是否已被运行时承载。
 *
 * 绝大多数特性会映射成 [BattleAbilityEffect]；`ground-immunity` 是少数例外，它不会返回效果对象，而是写入成员快照
 * 的接地事实。因此完整性测试不能简单要求所有 policy 都 `mapNotNull` 成效果，这里把“非效果但已承载”的例外也
 * 纳入统一判定。
 */
internal fun String.isBattleAbilityRuntimePolicySupported(elementIds: Map<String, Long>): Boolean =
	this == "ground-immunity" || toBattleAbilityEffect(elementIds) != null
