package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleRandomTraceEntry
import io.github.lishangbu.battlerules.dto.BattleActionViolationResponse
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnResponse

/**
 * 战斗沙盒规则命中摘要映射器。
 *
 * 沙盒响应中的 `ruleHits` 不是新的规则事实源，而是把本次结算已经产生的事件、行动违规和随机 trace 归入
 * 312 条规则覆盖账本使用的 12 个规则族。把映射集中在这里有两个目的：
 * - 运行时服务只负责结算和状态装配，不混入一大段展示分组表。
 * - 规则命中测试可以枚举 [io.github.lishangbu.battleengine.model.BattleEvent] 的全部事件类型，防止新增事件后
 *   管理页规则命中 silently 漏掉对应规则族。
 */
class BattleSandboxRuleHitMapper {
	fun summaries(
		currentEvents: List<BattleSandboxTurnResponse.Event>,
		violations: List<BattleActionViolationResponse>,
		randomTrace: List<BattleRandomTraceEntry>,
	): List<BattleSandboxTurnResponse.RuleHitSummary> {
		val eventHits = currentEvents.mapNotNull { event ->
			val familyCode = familyCodeForEventType(event.type) ?: return@mapNotNull null
			RuleHitKey(
				familyCode = familyCode,
				itemCode = event.type,
				itemName = event.typeLabel.ifBlank { event.type },
			)
		}
		val violationHits = violations.map { violation ->
			RuleHitKey(
				familyCode = TURN_FLOW_ACTION_ORDERING,
				itemCode = violation.code,
				itemName = violation.message.ifBlank { violation.code },
			)
		}
		val randomHits = randomTrace.map { trace ->
			RuleHitKey(
				familyCode = RANDOM_REPLAY_PUBLIC_REFERENCE,
				itemCode = trace.reason.toRandomRuleHitCode(),
				itemName = trace.reason.toRandomRuleHitName(),
			)
		}
		return (eventHits + violationHits + randomHits)
			.groupingBy { it }
			.eachCount()
			.map { (key, triggerCount) ->
				BattleSandboxTurnResponse.RuleHitSummary(
					familyCode = key.familyCode,
					familyName = familyName(key.familyCode),
					itemCode = key.itemCode,
					itemName = key.itemName,
					triggerCount = triggerCount,
				)
			}
			.sortedWith(compareBy({ it.familyCode }, { it.itemCode }))
	}

	fun familyCodeForEventType(eventType: String): String? =
		when (eventType) {
			"BattleStarted",
			"TurnStarted",
			"ParticipantSwitched",
			"ParticipantTerastallized",
			"TargetForcedSwitchSelected",
			"SwitchPrevented",
			"ParticipantFainted",
			"OpponentHeldItemRevealed",
			"OpponentSkillRevealed",
			"ParticipantTransformed",
			"DangerousOpponentSkillDetected",
			"TurnEnded",
			"BattleEnded" -> LIFECYCLE_SWITCH_FAINT_RESULT
			"SkillUsed",
			"SkillPpReduced",
			"MultiHitCountDetermined",
			"LockedMoveStarted",
			"LockedMoveAdvanced",
			"LockedMoveEnded",
			"SkillPrevented",
			"RechargeStarted",
			"SkillChargeStarted",
			"SkillChargeReleased",
			"SkillChargeInterrupted" -> TURN_FLOW_ACTION_ORDERING
			"SkillMissed",
			"AccuracyLockStarted",
			"ProtectionStarted",
			"ProtectionFailed",
			"ProtectionBroken",
			"FatalDamageEndureStarted",
			"SkillBlockedByProtection",
			"SkillBlockedByTerrain",
			"SkillBlockedByElement",
			"SubstituteStarted",
			"SubstituteDamageApplied",
			"SubstituteBroken",
			"SubstituteCleared",
			"FatalDamageSurvived" -> HIT_PROTECT_SUBSTITUTE_IMMUNITY_REFLECT
			"DamageApplied",
			"CriticalHitStageBoostStarted",
			"ParticipantElementsChanged",
			"RecoilDamageApplied",
			"ConfusionDamageApplied" -> DAMAGE_FORMULA_STAT_ELEMENT_ROUNDING
			"StatusApplied",
			"StatusApplicationBlocked",
			"StatusCleared",
			"VolatileStatusApplied",
			"VolatileStatusApplicationBlocked",
			"VolatileStatusCleared",
			"BindingDamageApplied",
			"LeechSeedPlanted",
			"LeechSeedDamageApplied",
			"LeechSeedCleared",
			"PerishCountdownStarted",
			"PerishCountdownAdvanced",
			"ResidualDamageApplied" -> MAJOR_VOLATILE_PERSISTENT_STATUS
			"SideDamageReductionStarted",
			"SideDamageReductionsRemoved",
			"SideProtectionStarted",
			"SideProtectionsRemoved",
			"SideSpeedModifierStarted",
			"SideEntryHazardChanged",
			"SideEntryHazardRemoved",
			"EntryHazardDamageApplied",
			"EntryHazardStatusApplied",
			"EntryHazardStatusApplicationBlocked",
			"EntryHazardStatStageChanged",
			"FieldSpeedOrderStarted",
			"FieldSpeedOrderEnded",
			"TerrainHealingApplied",
			"WeatherDamageApplied",
			"WeatherHealingApplied",
			"WeatherStarted",
			"WeatherEnded",
			"TerrainStarted",
			"TerrainEnded" -> WEATHER_TERRAIN_FIELD_SIDE_CONDITION
			"SkillFailed",
			"SkillDisabled",
			"StatStageChanged",
			"StatStageChangeBlocked",
			"WeightReductionChanged",
			"StatStageCleared",
			"StatStageCopied",
			"StatStageSwapped",
			"StatStageInverted",
			"HealingApplied",
			"LeechSeedHealingApplied",
			"SkillHealingApplied",
			"SkillRecoilDamageApplied",
			"SkillSelfSacrificeDamageApplied",
			"HpAveragedBySkill" -> SKILL_EFFECT_FAMILY
			"SkillBlockedByAbility",
			"SkillAbsorbedByAbility",
			"AbilityRetaliationDamageApplied" -> ABILITY_EFFECT_FAMILY
			"AbilityForcedSwitchSelected" -> ABILITY_EFFECT_FAMILY
			"AbilitySideDamageReductionsRemoved" -> ABILITY_EFFECT_FAMILY
			"ConsumedItemPickedUp" -> ABILITY_EFFECT_FAMILY
			"AbilityChanged",
			"AbilityElementsChanged",
			"TerrainElementIdentityChanged" -> ABILITY_EFFECT_FAMILY
			"HeldItemDamageApplied",
			"HeldItemTransferred",
			"HeldItemElementIdentityApplied",
			"HeldItemHighestStatBoostActivated",
			"CriticalHitStageBoostedByItem",
			"ItemForcedSwitchSelected",
			"SkillBlockedByItem",
			"DamageReducedByItem",
			"SkillChargeSkippedByItem" -> ITEM_EFFECT_FAMILY
			else -> null
		}

	fun ruleHitFamilyCodes(): List<String> =
		listOf(
			FORMAT_AND_TEAM_VALIDATION,
			LIFECYCLE_SWITCH_FAINT_RESULT,
			TURN_FLOW_ACTION_ORDERING,
			TARGET_SCOPE_REDIRECTION,
			HIT_PROTECT_SUBSTITUTE_IMMUNITY_REFLECT,
			DAMAGE_FORMULA_STAT_ELEMENT_ROUNDING,
			MAJOR_VOLATILE_PERSISTENT_STATUS,
			WEATHER_TERRAIN_FIELD_SIDE_CONDITION,
			SKILL_EFFECT_FAMILY,
			ABILITY_EFFECT_FAMILY,
			ITEM_EFFECT_FAMILY,
			RANDOM_REPLAY_PUBLIC_REFERENCE,
		)

	fun familyName(familyCode: String): String =
		when (familyCode) {
			FORMAT_AND_TEAM_VALIDATION -> "赛制与队伍校验"
			LIFECYCLE_SWITCH_FAINT_RESULT -> "生命周期、替换与胜负"
			TURN_FLOW_ACTION_ORDERING -> "回合流程与行动顺序"
			TARGET_SCOPE_REDIRECTION -> "目标范围与重定向"
			HIT_PROTECT_SUBSTITUTE_IMMUNITY_REFLECT -> "命中、防护、替身与免疫"
			DAMAGE_FORMULA_STAT_ELEMENT_ROUNDING -> "伤害公式、能力与属性"
			MAJOR_VOLATILE_PERSISTENT_STATUS -> "主要、临时与持续状态"
			WEATHER_TERRAIN_FIELD_SIDE_CONDITION -> "天气、场地与一侧条件"
			SKILL_EFFECT_FAMILY -> "技能效果族"
			ABILITY_EFFECT_FAMILY -> "特性效果族"
			ITEM_EFFECT_FAMILY -> "道具效果族"
			RANDOM_REPLAY_PUBLIC_REFERENCE -> "随机、回放与对照"
			else -> familyCode
		}

	private fun String.toRandomRuleHitCode(): String =
		when {
			startsWith("accuracy for ") -> "random-accuracy"
			startsWith("damage random for ") -> "random-damage"
			startsWith("critical hit for ") -> "random-critical-hit"
			startsWith("protection chance for ") -> "random-protection"
			startsWith("multi-hit count for ") -> "random-multi-hit"
			startsWith("locked move duration for ") -> "random-locked-move-duration"
			startsWith("random adjacent opponent target for ") -> "random-target"
			startsWith("forced switch target for ") -> "random-forced-switch-target"
			startsWith("confusion damage random for ") -> "random-confusion-damage"
			startsWith("binding duration for ") -> "random-binding-duration"
			else -> this
		}

	private fun String.toRandomRuleHitName(): String =
		when {
			startsWith("accuracy for ") -> "命中随机"
			startsWith("damage random for ") -> "伤害随机"
			startsWith("critical hit for ") -> "要害随机"
			startsWith("protection chance for ") -> "连续保护随机"
			startsWith("multi-hit count for ") -> "连击次数随机"
			startsWith("locked move duration for ") -> "锁招持续回合随机"
			startsWith("random adjacent opponent target for ") -> "随机目标选择"
			startsWith("forced switch target for ") -> "强制替换目标随机"
			startsWith("confusion damage random for ") -> "混乱自伤随机"
			startsWith("binding duration for ") -> "束缚持续回合随机"
			else -> "随机分支"
		}

	/**
	 * 聚合规则命中时使用的稳定分组键。
	 *
	 * `itemName` 参与键是有意为之：同一个后端 itemCode 如果未来被不同来源映射出不同中文名，摘要不能静默合并，
	 * 否则页面会隐藏文案漂移。
	 */
	private data class RuleHitKey(
		val familyCode: String,
		val itemCode: String,
		val itemName: String,
	)

	private companion object {
		const val FORMAT_AND_TEAM_VALIDATION = "format-and-team-validation"
		const val LIFECYCLE_SWITCH_FAINT_RESULT = "lifecycle-switch-faint-result"
		const val TURN_FLOW_ACTION_ORDERING = "turn-flow-action-ordering"
		const val TARGET_SCOPE_REDIRECTION = "target-scope-redirection"
		const val HIT_PROTECT_SUBSTITUTE_IMMUNITY_REFLECT = "hit-protect-substitute-immunity-reflect"
		const val DAMAGE_FORMULA_STAT_ELEMENT_ROUNDING = "damage-formula-stat-element-rounding"
		const val MAJOR_VOLATILE_PERSISTENT_STATUS = "major-volatile-persistent-status"
		const val WEATHER_TERRAIN_FIELD_SIDE_CONDITION = "weather-terrain-field-side-condition"
		const val SKILL_EFFECT_FAMILY = "skill-effect-family"
		const val ABILITY_EFFECT_FAMILY = "ability-effect-family"
		const val ITEM_EFFECT_FAMILY = "item-effect-family"
		const val RANDOM_REPLAY_PUBLIC_REFERENCE = "random-replay-public-reference"
	}
}
