package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleOneHitKnockOut
import io.github.lishangbu.battleengine.model.BattleProportionalDamage
import io.github.lishangbu.battleengine.model.BattleSkillDynamicPower
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillPostDamageStatusCure
import io.github.lishangbu.battleengine.model.BattleSkillPowerMultiplier
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleSkillWeightEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.common.web.invalidValue

/**
 * 技能运行时 policy 映射器。
 *
 * 本文件只处理 `battle_skill_rule` 主行上的 policy 字段：目标范围、固定/比例/HP 派生/一击必杀伤害、HP 后效、
 * 天气场地后效，以及启用技能规则的显式支持集合。技能子表里的状态、能力阶级和场地明细由
 * [BattleSkillRuleEffectRuntimeLookup] 读取；这里不读取数据库，也不关心服务层事务，只把稳定字符串翻译成
 * battle-engine 的强类型模型。
 */
internal fun String.toBattleSkillTargetScope(): BattleSkillTargetScope =
	when (this) {
		"selected-target" -> BattleSkillTargetScope.SELECTED_TARGET
		"self" -> BattleSkillTargetScope.SELF
		"all-opponents" -> BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS
		"all-adjacent-participants" -> BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS
		"random-opponent" -> BattleSkillTargetScope.RANDOM_ADJACENT_OPPONENT
		else -> invalidValue("targetPolicy", "不支持的技能目标策略: $this")
	}

internal fun String.toBattleSkillHpEffects(): List<BattleSkillHpEffect> =
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
		"drain-full-damage" -> listOf(
			BattleSkillHpEffect.DrainDamage(
				numerator = 1,
				denominator = 1,
			),
		)
		"recoil-quarter-damage" -> listOf(
			BattleSkillHpEffect.RecoilByDamageDealt(
				numerator = 1,
				denominator = 4,
			),
		)
		"recoil-third-damage" -> listOf(
			BattleSkillHpEffect.RecoilByDamageDealt(
				numerator = 1,
				denominator = 3,
			),
		)
		"recoil-half-damage" -> listOf(
			BattleSkillHpEffect.RecoilByDamageDealt(
				numerator = 1,
				denominator = 2,
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
		"sandstorm-self-heal-max-hp" -> listOf(
			BattleSkillHpEffect.SelfHealMaxHpByWeather(
				defaultFraction = BattleSkillHpEffect.HpFraction(1, 2),
				weatherFractions = mapOf(
					BattleWeather.SANDSTORM to BattleSkillHpEffect.HpFraction(2, 3),
				),
			),
		)
		"target-heal-half-max-hp" -> listOf(
			BattleSkillHpEffect.TargetHealMaxHpFraction(
				numerator = 1,
				denominator = 2,
			),
		)
		"self-heal-by-target-current-attack" -> listOf(BattleSkillHpEffect.SelfHealByTargetCurrentAttack)
		"target-major-status-cure-self-heal-half-max-hp" -> listOf(
			BattleSkillHpEffect.SelfHealAfterTargetMajorStatusCure(
				numerator = 1,
				denominator = 2,
			),
		)
		"terrain-target-heal-max-hp" -> listOf(
			BattleSkillHpEffect.TargetHealMaxHpByTerrain(
				defaultFraction = BattleSkillHpEffect.HpFraction(1, 2),
				terrainFractions = mapOf(
					BattleTerrain.GRASSY to BattleSkillHpEffect.HpFraction(2, 3),
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

internal fun String.toBattleFixedDamage(): BattleFixedDamage? =
	when (this) {
		"fixed-damage-20" -> BattleFixedDamage.FixedAmount(20)
		"fixed-damage-40" -> BattleFixedDamage.FixedAmount(40)
		"user-level-fixed-damage" -> BattleFixedDamage.UserLevel
		else -> null
	}

internal fun String.toBattleProportionalDamage(): BattleProportionalDamage? =
	when (this) {
		"target-current-hp-half-damage" -> BattleProportionalDamage.TargetCurrentHpFraction(
			numerator = 1,
			denominator = 2,
		)
		else -> null
	}

internal fun String.toBattleHpDerivedDamage(): BattleHpDerivedDamage? =
	when (this) {
		"target-hp-minus-user-hp-damage" -> BattleHpDerivedDamage.TargetCurrentHpMinusUserCurrentHp
		"user-current-hp-sacrifice-damage" -> BattleHpDerivedDamage.UserCurrentHpAndUserFaints
		else -> null
	}

internal fun String.toBattleOneHitKnockOut(): BattleOneHitKnockOut? =
	when (this) {
		"one-hit-knockout-damage" -> BattleOneHitKnockOut()
		"same-element-sensitive-one-hit-knockout-damage" -> BattleOneHitKnockOut(
			baseAccuracyPercent = 20,
			sameElementUserBaseAccuracyPercent = 30,
			blocksSameElementTarget = true,
		)
		else -> null
	}

internal fun String.toBattleSkillEnvironmentEffects(): List<BattleSkillEnvironmentEffect> =
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

/**
 * 将数据库中的技能运行态策略转换为临时体重变化效果。
 *
 * 这类规则不会改变资料表中的基础体重，只在单场战斗内累加运行态减重，并由引擎在离场时清除。当前只保留
 * “速度阶级实际提升后自身减重 100kg”这一条已经可由通用状态阶级事件验证的现代规则。
 */
internal fun String.toBattleSkillWeightEffects(): List<BattleSkillWeightEffect> =
	when (this) {
		"self-weight-reduction-100kg-after-speed-change" -> listOf(
			BattleSkillWeightEffect(
				target = BattleEffectTarget.USER,
				reduction = 1000,
				minimumWeight = 1,
				requiredChangedStat = BattleStat.SPEED,
			),
		)
		else -> emptyList()
	}

internal fun String.toBattleSkillPowerMultipliers(): List<BattleSkillPowerMultiplier> =
	when (this) {
		"power-double-if-user-burn-poison-paralysis" -> listOf(
			BattleSkillPowerMultiplier.UserMajorStatus(
				statuses = setOf(
					BattleMajorStatus.BURN,
					BattleMajorStatus.PARALYSIS,
					BattleMajorStatus.POISON,
					BattleMajorStatus.BAD_POISON,
				),
				multiplier = 2.0,
			),
		)
		"power-double-if-target-half-hp-or-less" -> listOf(
			BattleSkillPowerMultiplier.TargetCurrentHpAtMostFraction(
				numerator = 1,
				denominator = 2,
				multiplier = 2.0,
			),
		)
		"power-double-if-target-poisoned" -> listOf(
			BattleSkillPowerMultiplier.TargetMajorStatus(
				statuses = setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
				multiplier = 2.0,
			),
		)
		"power-double-if-target-paralysis-cure-target-paralysis-after-damage" -> listOf(
			BattleSkillPowerMultiplier.TargetMajorStatus(
				statuses = setOf(BattleMajorStatus.PARALYSIS),
				multiplier = 2.0,
			),
		)
		"power-double-if-target-sleep-cure-target-sleep-after-damage" -> listOf(
			BattleSkillPowerMultiplier.TargetMajorStatus(
				statuses = setOf(BattleMajorStatus.SLEEP),
				multiplier = 2.0,
			),
		)
		"power-double-if-target-major-status" -> listOf(
			BattleSkillPowerMultiplier.TargetMajorStatus(
				statuses = BattleMajorStatus.entries.toSet(),
				multiplier = 2.0,
			),
		)
		"power-double-if-user-has-no-held-item" -> listOf(
			BattleSkillPowerMultiplier.UserHasNoHeldItem(multiplier = 2.0),
		)
		else -> emptyList()
	}

internal fun String.toBattleSkillPostDamageStatusCures(): List<BattleSkillPostDamageStatusCure> =
	when (this) {
		"power-double-if-target-paralysis-cure-target-paralysis-after-damage" -> listOf(
			BattleSkillPostDamageStatusCure(statuses = setOf(BattleMajorStatus.PARALYSIS)),
		)
		"power-double-if-target-sleep-cure-target-sleep-after-damage" -> listOf(
			BattleSkillPostDamageStatusCure(statuses = setOf(BattleMajorStatus.SLEEP)),
		)
		"cure-target-burn-after-damage" -> listOf(
			BattleSkillPostDamageStatusCure(statuses = setOf(BattleMajorStatus.BURN)),
		)
		else -> emptyList()
	}

internal fun String.toBattleSkillDynamicPower(): BattleSkillDynamicPower? =
	when (this) {
		"power-by-user-positive-stat-stage-sum" -> BattleSkillDynamicPower.PositiveStatStageSum(
			source = BattleEffectTarget.USER,
			basePower = 20,
			powerPerPositiveStage = 20,
		)
		"power-by-target-positive-stat-stage-sum-max-200" -> BattleSkillDynamicPower.PositiveStatStageSum(
			source = BattleEffectTarget.TARGET,
			basePower = 60,
			powerPerPositiveStage = 20,
			maxPower = 200,
		)
		"power-by-user-target-speed-ratio" -> BattleSkillDynamicPower.UserSpeedRatioThresholds(
			thresholds = listOf(
				BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 4, power = 150),
				BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 3, power = 120),
				BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 2, power = 80),
				BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 1, power = 60),
			),
			fallbackPower = 40,
		)
		"power-by-target-user-speed-ratio-max-150" -> BattleSkillDynamicPower.TargetToUserSpeedRatio(
			multiplier = 25,
			additivePower = 1,
			maxPower = 150,
		)
		"power-by-target-weight-threshold" -> BattleSkillDynamicPower.TargetWeightThresholds(
			thresholds = listOf(
				BattleSkillDynamicPower.WeightPowerThreshold(maxWeightInclusive = 100, power = 20),
				BattleSkillDynamicPower.WeightPowerThreshold(maxWeightInclusive = 250, power = 40),
				BattleSkillDynamicPower.WeightPowerThreshold(maxWeightInclusive = 500, power = 60),
				BattleSkillDynamicPower.WeightPowerThreshold(maxWeightInclusive = 1000, power = 80),
				BattleSkillDynamicPower.WeightPowerThreshold(maxWeightInclusive = 2000, power = 100),
			),
			fallbackPower = 120,
		)
		"power-by-user-target-weight-ratio" -> BattleSkillDynamicPower.UserTargetWeightRatioThresholds(
			thresholds = listOf(
				BattleSkillDynamicPower.WeightRatioPowerThreshold(minimumUserToTargetRatio = 5, power = 120),
				BattleSkillDynamicPower.WeightRatioPowerThreshold(minimumUserToTargetRatio = 4, power = 100),
				BattleSkillDynamicPower.WeightRatioPowerThreshold(minimumUserToTargetRatio = 3, power = 80),
				BattleSkillDynamicPower.WeightRatioPowerThreshold(minimumUserToTargetRatio = 2, power = 60),
			),
			fallbackPower = 40,
		)
		else -> null
	}

internal fun String.ignoresUserBurnAttackReduction(): Boolean =
	this == "power-double-if-user-burn-poison-paralysis"

internal fun String.removesUserElementAfterDamage(): Boolean =
	this == "remove-user-element-after-damage"

private val battleSkillStructuralEffectPolicies = setOf(
	"standard-damage",
	"standard-damage-with-status",
	"standard-damage-with-stat",
	"standard-damage-clear-target-stat-stages",
	"standard-damage-with-force-switch",
	"multi-hit-damage",
	"protect-self",
	"stat-stage-change",
	"status-effect",
	"status-application",
	"side-condition",
	"side-entry-hazard",
	"field-condition",
	"force-target-switch",
	"apply-heal-block",
	"apply-taunt",
	"apply-disable",
	"apply-torment",
	"clear-all-active-stat-stages",
	"copy-target-stat-stages",
	"swap-attack-stat-stages",
	"swap-defense-stat-stages",
	"swap-all-stat-stages",
	"invert-target-stat-stages",
)

private val battleSkillTargetPolicies = setOf(
	"selected-target",
	"self",
	"all-opponents",
	"all-adjacent-participants",
	"random-opponent",
)

private val battleSkillHitPolicies = setOf(
	"standard-hit",
	"always-hit",
	"protect-hit",
	"multi-hit",
)

private val battleSkillDamagePolicies = setOf(
	"standard-damage",
	"no-damage",
	"fixed-damage",
	"proportional-damage",
	"hp-derived-damage",
	"one-hit-knockout-damage",
	"status-effect",
)

/**
 * 判断技能 `effect_policy` 是否已经被运行时装配层承载。
 *
 * 技能 policy 有两种形态：
 * - 直接映射型，例如固定伤害、吸取、天气/场地设置，会在本 mapper 中转换成 battle-engine 的强类型效果。
 * - 结构型，例如基础变化技能、能力阶级、强制换人、挑衅/定身法/无理取闹等，实际参数保存在主行、技能子表
 *   或布尔字段中，[BattleSkillRuntimeLookup] 会把这些字段一起装配进
 *   [io.github.lishangbu.battleengine.model.BattleSkillSlot]。
 *
 * 这个函数专门服务于运行时完整性测试：Liquibase 里只要启用了新的技能 policy，就必须落在上述两类之一，避免
 * 新资料因为拼写或 mapper 漏补而被装配流程悄悄忽略。
 */
internal fun String.isBattleSkillRuntimeEffectPolicySupported(): Boolean =
	this in battleSkillStructuralEffectPolicies ||
		toBattleSkillHpEffects().isNotEmpty() ||
	toBattleFixedDamage() != null ||
	toBattleProportionalDamage() != null ||
	toBattleHpDerivedDamage() != null ||
	toBattleOneHitKnockOut() != null ||
	toBattleSkillEnvironmentEffects().isNotEmpty() ||
	toBattleSkillPowerMultipliers().isNotEmpty() ||
	toBattleSkillPostDamageStatusCures().isNotEmpty() ||
	toBattleSkillDynamicPower() != null ||
	toBattleSkillWeightEffects().isNotEmpty() ||
	removesUserElementAfterDamage()

/**
 * 判断技能目标 policy 是否属于运行时装配层的显式目标集合。
 *
 * 启用中的数据行必须显式落在本集合中；缺失或拼错的目标策略会在运行时装配时直接失败，避免范围技能被静默降级
 * 为单体技能。
 */
internal fun String.isBattleSkillRuntimeTargetPolicySupported(): Boolean =
	this in battleSkillTargetPolicies

/**
 * 判断命中 policy 是否属于运行时可识别集合。
 *
 * 当前命中 policy 主要作为资料侧约束和测试追踪点：普通命中、必中、保护、连续命中分别会由技能命中流程、保护流程
 * 和多段命中字段共同承载。这里用显式集合把“资料 code 可识别”这件事固定下来。
 */
internal fun String.isBattleSkillRuntimeHitPolicySupported(): Boolean =
	this in battleSkillHitPolicies

/**
 * 判断伤害 policy 是否属于运行时可识别集合。
 *
 * 伤害 policy 决定技能走普通伤害、无伤害、固定伤害、比例伤害、体力差伤害或纯状态路径；测试会读取数据库中的启用
 * 数据并调用本函数，防止新伤害路径只写入了 Liquibase、没有同步接入运行时。
 */
internal fun String.isBattleSkillRuntimeDamagePolicySupported(): Boolean =
	this in battleSkillDamagePolicies
