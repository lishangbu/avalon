package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierKind
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageOperationKind
import io.github.lishangbu.battleengine.model.BattleStatStageOperationTarget
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.common.web.invalidValue

/**
 * 战斗运行时通用 code 映射器。
 *
 * `battle-rules` 的三范式资料表会保存稳定的 `effect_policy`、`target_policy`、天气 code、状态 code 等短文本。
 * 这些短文本适合数据库维护和 Liquibase 种子数据审阅，但纯战斗引擎不能在结算时到处解析字符串，否则规则顺序、
 * 类型约束和错误位置都会变得模糊。本文件只保存技能、特性、道具都会复用的通用 code 翻译；
 * 具体 policy 映射分别放在 BattleSkillPolicyMapper、BattleAbilityPolicyMapper 和 BattleItemPolicyMapper。
 *
 * 这里故意保持为顶层扩展函数，而不是接口或可插拔注册表：当前策略集合来自本项目自己的资料表，运行时没有替换实现
 * 的需求。新增规则时在这里补一条确定映射，并用对应引擎测试证明它被结算即可。
 */
internal fun Map<String, Long>.requiredElementId(code: String): Long =
	this[code] ?: error("核心属性资料缺失: $code")

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

internal fun String.toBattleTerrain(): BattleTerrain =
	when (this) {
		"electric-terrain" -> BattleTerrain.ELECTRIC
		"grassy-terrain" -> BattleTerrain.GRASSY
		"misty-terrain" -> BattleTerrain.MISTY
		"psychic-terrain" -> BattleTerrain.PSYCHIC
		else -> invalidValue("terrainRuleId", "不支持的场地规则: $this")
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
