package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderApplication
import io.github.lishangbu.battleengine.model.BattleSideConditionApplication
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardApplication
import io.github.lishangbu.battleengine.model.BattleSideProtectionApplication
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierApplication
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.model.BattleWeather

/**
 * 一条技能规则全部多行效果的运行时快照。
 *
 * 该快照是 `battle-rules` 交给纯引擎前的中间装配结果，不是持久化模型或 Controller 响应。字段顺序贴近技能槽，
 * 便于核对每类规则子表是否完整进入引擎。
 */
data class BattleSkillRuleEffectRuntimeSnapshot(
	val chargeSkippedByWeathers: Set<BattleWeather>,
	val accuracyOverridesByWeather: Map<BattleWeather, Int?>,
	val powerMultipliersByWeather: Map<BattleWeather, Double>,
	val groundedPowerMultipliersByTerrain: Map<BattleTerrain, Double>,
	val elementOverridesByWeather: Map<BattleWeather, Long>,
	val elementOverridesByTerrain: Map<BattleTerrain, Long>,
	val statusApplications: List<BattleStatusApplication>,
	val volatileStatusApplications: List<BattleVolatileStatusApplication>,
	val statStageEffects: List<BattleStatStageEffect>,
	val statStageOperations: List<BattleStatStageOperation>,
	val sideConditionApplications: List<BattleSideConditionApplication>,
	val sideSpeedModifierApplications: List<BattleSideSpeedModifierApplication>,
	val sideEntryHazardApplications: List<BattleSideEntryHazardApplication>,
	val sideProtectionApplications: List<BattleSideProtectionApplication>,
	val fieldSpeedOrderApplications: List<BattleFieldSpeedOrderApplication>,
)
