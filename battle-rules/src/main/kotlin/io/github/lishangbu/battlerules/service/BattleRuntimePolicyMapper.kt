package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleProportionalDamage
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierKind
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageOperationKind
import io.github.lishangbu.battleengine.model.BattleStatStageOperationTarget
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.common.web.invalidValue

/**
 * 战斗运行时策略映射器。
 *
 * `battle-rules` 的三范式资料表会保存稳定的 `effect_policy`、`target_policy`、天气 code、状态 code 等短文本。
 * 这些短文本适合数据库维护和 Liquibase 种子数据审阅，但纯战斗引擎不能在结算时到处解析字符串，否则规则顺序、
 * 类型约束和错误位置都会变得模糊。本文件集中完成“资料 code -> battle-engine 强类型模型”的翻译，让
 * [BattleRuntimeSnapshotService] 只负责事务、查询和装配流程。
 *
 * 这里故意保持为顶层扩展函数，而不是接口或可插拔注册表：当前策略集合来自本项目自己的资料表，运行时没有替换实现
 * 的需求。新增规则时在这里补一条确定映射，并用对应引擎测试证明它被结算即可。
 */
internal fun Map<String, Long>.requiredElementId(code: String): Long =
	this[code] ?: error("核心属性资料缺失: $code")

internal fun String?.toBattleSkillTargetScope(): BattleSkillTargetScope =
	when (this) {
		"all-opponents" -> BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS
		"all-adjacent-participants" -> BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS
		"random-opponent" -> BattleSkillTargetScope.RANDOM_ADJACENT_OPPONENT
		else -> BattleSkillTargetScope.SELECTED_TARGET
	}

internal fun String?.toBattleSkillHpEffects(): List<BattleSkillHpEffect> =
	when (this) {
		"drain-half-damage" -> listOf(
			BattleSkillHpEffect.DrainDamage(
				numerator = 1,
				denominator = 2,
			),
		)
		"drain-three-quarter-damage" -> listOf(
			BattleSkillHpEffect.DrainDamage(
				numerator = 3,
				denominator = 4,
			),
		)
		"recoil-third-damage" -> listOf(
			BattleSkillHpEffect.RecoilByDamageDealt(
				numerator = 1,
				denominator = 3,
			),
		)
		"self-heal-half-max-hp" -> listOf(
			BattleSkillHpEffect.SelfHealMaxHpFraction(
				numerator = 1,
				denominator = 2,
			),
		)
		"weather-self-heal-max-hp" -> listOf(
			BattleSkillHpEffect.SelfHealMaxHpByWeather(
				defaultFraction = BattleSkillHpEffect.HpFraction(1, 2),
				weatherFractions = mapOf(
					BattleWeather.SUN to BattleSkillHpEffect.HpFraction(2, 3),
					BattleWeather.RAIN to BattleSkillHpEffect.HpFraction(1, 4),
					BattleWeather.SANDSTORM to BattleSkillHpEffect.HpFraction(1, 4),
					BattleWeather.SNOW to BattleSkillHpEffect.HpFraction(1, 4),
				),
			),
		)
		"create-substitute-quarter-max-hp" -> listOf(
			BattleSkillHpEffect.CreateSubstitute(
				numerator = 1,
				denominator = 4,
			),
		)
		else -> emptyList()
	}

internal fun String?.toBattleFixedDamage(): BattleFixedDamage? =
	when (this) {
		"fixed-damage-20" -> BattleFixedDamage.FixedAmount(20)
		"fixed-damage-40" -> BattleFixedDamage.FixedAmount(40)
		"user-level-fixed-damage" -> BattleFixedDamage.UserLevel
		else -> null
	}

internal fun String?.toBattleProportionalDamage(): BattleProportionalDamage? =
	when (this) {
		"target-current-hp-half-damage" -> BattleProportionalDamage.TargetCurrentHpFraction(
			numerator = 1,
			denominator = 2,
		)
		else -> null
	}

internal fun String?.toBattleHpDerivedDamage(): BattleHpDerivedDamage? =
	when (this) {
		"target-hp-minus-user-hp-damage" -> BattleHpDerivedDamage.TargetCurrentHpMinusUserCurrentHp
		"user-current-hp-sacrifice-damage" -> BattleHpDerivedDamage.UserCurrentHpAndUserFaints
		else -> null
	}

internal fun String?.toBattleSkillEnvironmentEffects(): List<BattleSkillEnvironmentEffect> =
	when (this) {
		"set-terrain-electric" -> listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.ELECTRIC))
		"set-terrain-grassy" -> listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.GRASSY))
		"set-terrain-misty" -> listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.MISTY))
		"set-terrain-psychic" -> listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.PSYCHIC))
		"set-weather-rain" -> listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.RAIN))
		"set-weather-sandstorm" -> listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.SANDSTORM))
		"set-weather-snow" -> listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.SNOW))
		"set-weather-sun" -> listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.SUN))
		else -> emptyList()
	}

internal fun String.toBattleDamageClass(): BattleDamageClass =
	when (this) {
		"physical" -> BattleDamageClass.PHYSICAL
		"special" -> BattleDamageClass.SPECIAL
		"status" -> BattleDamageClass.STATUS
		else -> invalidValue("damageClass", "不支持的技能伤害分类: $this")
	}

internal fun String.toBattleWeather(): BattleWeather =
	when (this) {
		"harsh-sunlight" -> BattleWeather.SUN
		"rain" -> BattleWeather.RAIN
		"sandstorm" -> BattleWeather.SANDSTORM
		"snow" -> BattleWeather.SNOW
		else -> invalidValue("weatherRuleId", "不支持的天气规则: $this")
	}

internal fun String.toBattleSideConditionTarget(): BattleSideConditionTarget =
	when (this) {
		"USER_SIDE" -> BattleSideConditionTarget.USER_SIDE
		"TARGET_SIDE" -> BattleSideConditionTarget.TARGET_SIDE
		else -> invalidValue("targetSide", "不支持的场上效果作用侧: $this")
	}

internal fun String.toBattleSideDamageReductionKind(): BattleSideDamageReductionKind? =
	when (this) {
		"side-reflect" -> BattleSideDamageReductionKind.PHYSICAL
		"side-light-screen" -> BattleSideDamageReductionKind.SPECIAL
		"side-aurora-veil" -> BattleSideDamageReductionKind.ALL_STANDARD_DAMAGE
		else -> null
	}

internal fun String.toBattleSideSpeedModifierKind(): BattleSideSpeedModifierKind? =
	when (this) {
		"side-tailwind" -> BattleSideSpeedModifierKind.TAILWIND
		else -> null
	}

internal fun String.toBattleSideEntryHazardKind(): BattleSideEntryHazardKind? =
	when (this) {
		"hazard-stealth-rock" -> BattleSideEntryHazardKind.STEALTH_ROCK
		"hazard-spikes" -> BattleSideEntryHazardKind.SPIKES
		"hazard-toxic-spikes" -> BattleSideEntryHazardKind.TOXIC_SPIKES
		"hazard-sticky-web" -> BattleSideEntryHazardKind.STICKY_WEB
		else -> null
	}

internal fun String.toBattleFieldSpeedOrderKind(): BattleFieldSpeedOrderKind? =
	when (this) {
		"field-trick-room" -> BattleFieldSpeedOrderKind.TRICK_ROOM
		else -> null
	}

internal fun String.toBattleEffectTarget(): BattleEffectTarget? =
	when (this) {
		"USER" -> BattleEffectTarget.USER
		"TARGET" -> BattleEffectTarget.TARGET
		// 技能执行器已经按实际命中的目标逐个调用附加效果；资料中的全体对手在这里映射为
		// “当前实际目标”，避免范围技能在效果层再次展开后重复结算。
		"ALL_OPPONENTS" -> BattleEffectTarget.TARGET
		else -> null
	}

internal fun String.toBattleStatStageOperationKind(): BattleStatStageOperationKind =
	when (this) {
		"CLEAR" -> BattleStatStageOperationKind.CLEAR
		"COPY" -> BattleStatStageOperationKind.COPY
		"SWAP" -> BattleStatStageOperationKind.SWAP
		"INVERT" -> BattleStatStageOperationKind.INVERT
		else -> invalidValue("operationKind", "不支持的能力阶级操作类型: $this")
	}

internal fun String.toBattleStatStageOperationTarget(): BattleStatStageOperationTarget =
	when (this) {
		"USER" -> BattleStatStageOperationTarget.USER
		"TARGET" -> BattleStatStageOperationTarget.TARGET
		"ALL_ACTIVE" -> BattleStatStageOperationTarget.ALL_ACTIVE
		else -> invalidValue("targetScope", "不支持的能力阶级操作目标: $this")
	}

internal fun String.toBattleMajorStatus(): BattleMajorStatus =
	when (this) {
		"burn" -> BattleMajorStatus.BURN
		"paralysis" -> BattleMajorStatus.PARALYSIS
		"poison" -> BattleMajorStatus.POISON
		"bad-poison" -> BattleMajorStatus.BAD_POISON
		"sleep" -> BattleMajorStatus.SLEEP
		"freeze" -> BattleMajorStatus.FREEZE
		else -> invalidValue("statusRuleId", "不支持的主要异常状态: $this")
	}

internal fun String.toBattleVolatileStatus(): BattleVolatileStatus =
	when (this) {
		"confusion" -> BattleVolatileStatus.CONFUSION
		"flinch" -> BattleVolatileStatus.FLINCH
		"heal-block" -> BattleVolatileStatus.HEAL_BLOCK
		"taunt" -> BattleVolatileStatus.TAUNT
		"disable" -> BattleVolatileStatus.DISABLE
		"torment" -> BattleVolatileStatus.TORMENT
		"binding" -> BattleVolatileStatus.BINDING
		else -> invalidValue("statusRuleId", "不支持的临时状态: $this")
	}

internal fun String.toBattleStat(): BattleStat =
	when (this) {
		"attack" -> BattleStat.ATTACK
		"defense" -> BattleStat.DEFENSE
		"special-attack" -> BattleStat.SPECIAL_ATTACK
		"special-defense" -> BattleStat.SPECIAL_DEFENSE
		"speed" -> BattleStat.SPEED
		"accuracy" -> BattleStat.ACCURACY
		"evasion" -> BattleStat.EVASION
		else -> invalidValue("statId", "不支持的战斗能力项: $this")
	}

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
		"element-damage-boost-normal" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("normal"),
			multiplier = 1.2,
		)
		"element-damage-boost-fighting" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("fighting"),
			multiplier = 1.2,
		)
		"element-damage-boost-flying" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("flying"),
			multiplier = 1.2,
		)
		"element-damage-boost-poison" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("poison"),
			multiplier = 1.2,
		)
		"element-damage-boost-ground" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("ground"),
			multiplier = 1.2,
		)
		"element-damage-boost-rock" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("rock"),
			multiplier = 1.2,
		)
		"element-damage-boost-bug" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("bug"),
			multiplier = 1.2,
		)
		"element-damage-boost-ghost" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("ghost"),
			multiplier = 1.2,
		)
		"element-damage-boost-steel" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("steel"),
			multiplier = 1.2,
		)
		"element-damage-boost-fire" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("fire"),
			multiplier = 1.2,
		)
		"element-damage-boost-water" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("water"),
			multiplier = 1.2,
		)
		"element-damage-boost-grass" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("grass"),
			multiplier = 1.2,
		)
		"element-damage-boost-electric" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("electric"),
			multiplier = 1.2,
		)
		"element-damage-boost-psychic" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("psychic"),
			multiplier = 1.2,
		)
		"element-damage-boost-ice" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("ice"),
			multiplier = 1.2,
		)
		"element-damage-boost-dragon" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("dragon"),
			multiplier = 1.2,
		)
		"element-damage-boost-dark" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("dark"),
			multiplier = 1.2,
		)
		"element-damage-boost-fairy" -> BattleItemEffect.ElementDamageBoost(
			elementId = elementIds.requiredElementId("fairy"),
			multiplier = 1.2,
		)
		"element-damage-reduction-normal" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("normal"),
			multiplier = 0.5,
			requiresSuperEffective = false,
		)
		"element-damage-reduction-fighting" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("fighting"),
			multiplier = 0.5,
		)
		"element-damage-reduction-flying" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("flying"),
			multiplier = 0.5,
		)
		"element-damage-reduction-poison" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("poison"),
			multiplier = 0.5,
		)
		"element-damage-reduction-ground" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("ground"),
			multiplier = 0.5,
		)
		"element-damage-reduction-rock" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("rock"),
			multiplier = 0.5,
		)
		"element-damage-reduction-bug" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("bug"),
			multiplier = 0.5,
		)
		"element-damage-reduction-ghost" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("ghost"),
			multiplier = 0.5,
		)
		"element-damage-reduction-steel" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("steel"),
			multiplier = 0.5,
		)
		"element-damage-reduction-fire" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("fire"),
			multiplier = 0.5,
		)
		"element-damage-reduction-water" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("water"),
			multiplier = 0.5,
		)
		"element-damage-reduction-grass" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("grass"),
			multiplier = 0.5,
		)
		"element-damage-reduction-electric" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("electric"),
			multiplier = 0.5,
		)
		"element-damage-reduction-psychic" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("psychic"),
			multiplier = 0.5,
		)
		"element-damage-reduction-ice" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("ice"),
			multiplier = 0.5,
		)
		"element-damage-reduction-dragon" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("dragon"),
			multiplier = 0.5,
		)
		"element-damage-reduction-dark" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("dark"),
			multiplier = 0.5,
		)
		"element-damage-reduction-fairy" -> BattleItemEffect.ElementDamageReduction(
			elementId = elementIds.requiredElementId("fairy"),
			multiplier = 0.5,
		)
		"small-berry-heal" -> BattleItemEffect.LowHpHeal(fixedHealAmount = 10)
		"medium-berry-heal" -> BattleItemEffect.LowHpHeal(healDenominator = 4)
		"choice-speed-lock" -> BattleItemEffect.ChoiceSkillLock(speedMultiplier = 1.5)
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
		else -> null
	}
