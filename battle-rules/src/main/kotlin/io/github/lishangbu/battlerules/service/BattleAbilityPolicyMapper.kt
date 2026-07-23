package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind

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
		"contact-damage-to-attacker-eighth" -> BattleAbilityEffect.ContactDamageToAttacker(damageDenominator = 8)
		"switch-in-opponents-attack-down" -> BattleAbilityEffect.SwitchInStatStageChange(
			stat = BattleStat.ATTACK,
			stageDelta = -1,
		)
		"switch-in-weather-rain" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.RAIN)
		"switch-in-weather-sandstorm" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.SANDSTORM)
		"switch-in-weather-snow" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.SNOW)
		"switch-in-weather-sun" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.SUN)
		"switch-in-reveal-opponent-held-items" -> BattleAbilityEffect.SwitchInRevealOpponentHeldItems()
		"switch-in-reveal-opponent-highest-power-skill" ->
			BattleAbilityEffect.SwitchInRevealOpponentHighestPowerSkill()
		"switch-in-transform-into-opponent" -> BattleAbilityEffect.SwitchInTransformIntoOpponent()
		"switch-in-disguise-as-last-healthy-ally" -> BattleAbilityEffect.SwitchInDisguiseAsLastHealthyAlly()
		"switch-in-detect-dangerous-opponent-skill" ->
			BattleAbilityEffect.SwitchInDetectDangerousOpponentSkill()
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
		"terrain-element-identity" -> BattleAbilityEffect.TerrainElementIdentity(
			mapOf(
				BattleTerrain.ELECTRIC to elementIds.requiredElementId("electric"),
				BattleTerrain.GRASSY to elementIds.requiredElementId("grass"),
				BattleTerrain.MISTY to elementIds.requiredElementId("fairy"),
				BattleTerrain.PSYCHIC to elementIds.requiredElementId("psychic"),
			),
		)
		"target-gender-damage-five-quarters-three-quarters" ->
			BattleAbilityEffect.TargetGenderDamageMultiplier()
		"contact-opposite-gender-infatuation-thirty-percent" ->
			BattleAbilityEffect.ContactInfatuationOnAttacker()
		"weather-heal-rain-eighth" -> BattleAbilityEffect.WeatherEndTurnHeal(
			weathers = setOf(BattleWeather.RAIN),
			healDenominator = 8,
		)
		"weather-heal-snow" -> BattleAbilityEffect.WeatherEndTurnHeal(
			weathers = setOf(BattleWeather.SNOW),
			healDenominator = 16,
		)
		"critical-hit-immunity" -> BattleAbilityEffect.CriticalHitImmunity()
		"full-hp-fatal-damage-survival" -> BattleAbilityEffect.SurviveFatalDamageAtFullHp()
		"indirect-damage-immunity" -> BattleAbilityEffect.IndirectDamageImmunity()
		"weather-damage-immunity-sandstorm" -> BattleAbilityEffect.WeatherDamageImmunity(
			weathers = setOf(BattleWeather.SANDSTORM),
		)
		"skill-recoil-damage-immunity" -> BattleAbilityEffect.SkillRecoilDamageImmunity()
		"ignore-opponent-accuracy-stat-stages" -> BattleAbilityEffect.IgnoreOpponentAccuracyStatStages()
		"ignore-opponent-damage-stat-stages" -> BattleAbilityEffect.IgnoreOpponentDamageStatStages()
		"ignore-target-ability-effects" -> BattleAbilityEffect.IgnoreTargetAbilityEffects()
		"status-skill-moves-last-ignore-target-ability" ->
			BattleAbilityEffect.StatusSkillMovesLastAndIgnoresTargetAbility()
		"contact-skill-protection-bypass" -> BattleAbilityEffect.ContactSkillProtectionBypass()
		"sound-based-skill-immunity" -> BattleAbilityEffect.SoundBasedSkillImmunity()
		"side-priority-move-immunity" -> BattleAbilityEffect.PriorityMoveImmunityForSide()
		"status-skill-priority-boost" -> BattleAbilityEffect.StatusSkillPriorityBoost()
		"full-hp-flying-skill-priority-plus-one" -> BattleAbilityEffect.ElementSkillPriorityBoost(
			elementIds.requiredElementId("flying"),
			1,
			requiresFullHp = true,
		)
		"weather-effect-suppression" -> BattleAbilityEffect.WeatherEffectSuppression()
		"healing-skill-priority-plus-three" -> BattleAbilityEffect.HealingSkillPriorityBoost(3)
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
		"stat-drop-immunity-all" -> BattleAbilityEffect.OpponentStatStageReductionImmunity(BattleStat.entries.toSet())
		"stat-drop-immunity-attack" -> BattleAbilityEffect.OpponentStatStageReductionImmunity(setOf(BattleStat.ATTACK))
		"stat-drop-immunity-defense" -> BattleAbilityEffect.OpponentStatStageReductionImmunity(setOf(BattleStat.DEFENSE))
		"stat-drop-immunity-accuracy" -> BattleAbilityEffect.OpponentStatStageReductionImmunity(setOf(BattleStat.ACCURACY))
		"stat-stage-delta-reverse" -> BattleAbilityEffect.StatStageDeltaMultiplier(-1)
		"stat-stage-delta-double" -> BattleAbilityEffect.StatStageDeltaMultiplier(2)
		"opponent-stat-drop-attack-plus-two" ->
			BattleAbilityEffect.OpponentStatReductionReactiveBoost(BattleStat.ATTACK, 2)
		"opponent-stat-drop-special-attack-plus-two" ->
			BattleAbilityEffect.OpponentStatReductionReactiveBoost(BattleStat.SPECIAL_ATTACK, 2)
		"major-status-immunity-poison" -> BattleAbilityEffect.MajorStatusImmunity(
			setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
		)
		"switch-in-self-attack-plus-one" -> BattleAbilityEffect.SwitchInStatStageChange(
			BattleStat.ATTACK,
			1,
			io.github.lishangbu.battleengine.model.BattleEffectTarget.USER,
		)
		"switch-in-self-defense-plus-one" -> BattleAbilityEffect.SwitchInStatStageChange(
			BattleStat.DEFENSE,
			1,
			io.github.lishangbu.battleengine.model.BattleEffectTarget.USER,
		)
		"switch-in-opponents-evasion-minus-one" -> BattleAbilityEffect.SwitchInStatStageChange(
			BattleStat.EVASION,
			-1,
		)
		"switch-in-opponent-defense-comparison-attack-plus-one" ->
			BattleAbilityEffect.SwitchInOpponentDefenseComparisonBoost()
		"switch-in-ally-heal-quarter" -> BattleAbilityEffect.SwitchInAllyHeal(4)
		"switch-in-ally-stat-stage-copy" -> BattleAbilityEffect.SwitchInAllyStatStageCopy()
		"switch-in-ally-stat-stage-reset" -> BattleAbilityEffect.SwitchInAllyStatStageReset()
		"switch-in-clear-all-side-damage-reductions" -> BattleAbilityEffect.SwitchInClearAllSideDamageReductions()
		"ally-special-damage-boost-thirteen-tenths" -> BattleAbilityEffect.AllySkillDamageBoost(
			1.3,
			setOf(BattleDamageClass.SPECIAL),
		)
		"ally-damage-boost-thirteen-tenths" -> BattleAbilityEffect.AllySkillDamageBoost(1.3)
		"ally-received-damage-three-quarters" -> BattleAbilityEffect.AllyReceivedDamageReduction(0.75)
		"ally-group-plus-minus-membership" -> BattleAbilityEffect.AllyAbilityGroupMembership("plus-minus")
		"ally-group-plus-minus-special-attack-one-and-half" ->
			BattleAbilityEffect.AllyAbilityPresenceAttackingStatMultiplier(
				groupCode = "plus-minus",
				stat = BattleStat.SPECIAL_ATTACK,
				multiplier = 1.5,
			)
		"ally-damage-immunity" -> BattleAbilityEffect.AllyDamageImmunity()
		"normal-to-flying-damage-boost" -> BattleAbilityEffect.SkillElementOverride(
			elementId = elementIds.requiredElementId("flying"),
			originalElementIds = setOf(elementIds.requiredElementId("normal")),
			damageMultiplier = 1.2,
		)
		"normal-to-electric-damage-boost" -> BattleAbilityEffect.SkillElementOverride(
			elementId = elementIds.requiredElementId("electric"),
			originalElementIds = setOf(elementIds.requiredElementId("normal")),
			damageMultiplier = 1.2,
		)
		"normal-to-fairy-damage-boost" -> BattleAbilityEffect.SkillElementOverride(
			elementId = elementIds.requiredElementId("fairy"),
			originalElementIds = setOf(elementIds.requiredElementId("normal")),
			damageMultiplier = 1.2,
		)
		"normal-to-ice-damage-boost" -> BattleAbilityEffect.SkillElementOverride(
			elementId = elementIds.requiredElementId("ice"),
			originalElementIds = setOf(elementIds.requiredElementId("normal")),
			damageMultiplier = 1.2,
		)
		"all-to-normal-damage-boost" -> BattleAbilityEffect.SkillElementOverride(
			elementId = elementIds.requiredElementId("normal"),
			damageMultiplier = 1.2,
		)
		"sound-to-water" -> BattleAbilityEffect.SkillElementOverride(
			elementId = elementIds.requiredElementId("water"),
			requiresSoundBased = true,
		)
		"opponent-barrier-bypass" -> BattleAbilityEffect.OpponentBarrierBypass()
		"additional-flinch-chance-ten-percent" -> BattleAbilityEffect.AdditionalFlinchChance(10)
		"secondary-effect-chance-double" -> BattleAbilityEffect.SecondaryEffectChanceMultiplier(2.0)
		"held-item-transfer-immunity" -> BattleAbilityEffect.HeldItemTransferImmunity()
		"held-item-removal-immunity" -> BattleAbilityEffect.HeldItemRemovalImmunity()
		"held-item-element-identity" -> BattleAbilityEffect.HeldItemElementIdentity()
		"explosion-effect-suppression" -> BattleAbilityEffect.ExplosionEffectSuppression()
		"drain-healing-reversal" -> BattleAbilityEffect.DrainHealingReversal()
		"fainted-ally-damage-boost-tenth-up-to-five" -> BattleAbilityEffect.FaintedAllyDamageBoost(0.1, 5)
		"switched-in-target-damage-double" -> BattleAbilityEffect.SwitchedInTargetDamageBoost(2.0)
		"target-already-acted-damage-thirteen-tenths" -> BattleAbilityEffect.TargetAlreadyActedDamageBoost(1.3)
		"forced-last-action-order" -> BattleAbilityEffect.ForcedLastActionOrder()
		"random-action-order-boost-thirty-percent" -> BattleAbilityEffect.RandomActionOrderBoost(30)
		"first-five-turns-attack-speed-half" -> BattleAbilityEffect.InitialActiveTurnsStatMultiplier(
			turns = 5,
			stats = setOf(BattleStat.ATTACK, BattleStat.SPEED),
			multiplier = 0.5,
		)
		"full-hp-effectiveness-half" -> BattleAbilityEffect.FullHpEffectivenessOverride(0.5)
		"received-physical-damage-opponent-toxic-spikes" ->
			BattleAbilityEffect.ReceivedPhysicalDamageOpponentSideHazard(BattleSideEntryHazardKind.TOXIC_SPIKES)
		"side-volatile-status-immunity-aroma-veil" -> BattleAbilityEffect.SideVolatileStatusImmunity(
			setOf(
				io.github.lishangbu.battleengine.model.BattleVolatileStatus.TAUNT,
				io.github.lishangbu.battleengine.model.BattleVolatileStatus.TORMENT,
				io.github.lishangbu.battleengine.model.BattleVolatileStatus.INFATUATION,
				io.github.lishangbu.battleengine.model.BattleVolatileStatus.DISABLE,
				io.github.lishangbu.battleengine.model.BattleVolatileStatus.HEAL_BLOCK,
			),
		)
		"projectile-skill-immunity" -> BattleAbilityEffect.ProjectileSkillImmunity()
		"pulse-based-skill-damage-boost-one-and-half" -> BattleAbilityEffect.PulseBasedSkillDamageBoost(1.5)
		"bite-based-skill-damage-boost-one-and-half" -> BattleAbilityEffect.BiteBasedSkillDamageBoost(1.5)
		"damaging-skill-steal-target-held-item" -> BattleAbilityEffect.DamagingSkillStealTargetHeldItem()
		"contact-steal-attacker-held-item" -> BattleAbilityEffect.ContactStealAttackerHeldItem()
		"received-damage-element-change" -> BattleAbilityEffect.ReceivedDamageElementChange()
		"received-damage-next-electric-damage-double" ->
			BattleAbilityEffect.ReceivedDamageNextElementDamageBoost(elementIds.requiredElementId("electric"), 2.0)
		"received-wind-damage-next-electric-damage-double" ->
			BattleAbilityEffect.ReceivedDamageNextElementDamageBoost(
				elementIds.requiredElementId("electric"),
				2.0,
				windOnly = true,
			)
		"wind-skill-immunity-attack-plus-one" ->
			BattleAbilityEffect.WindSkillImmunityStatStageChange(BattleStat.ATTACK, 1)
		"every-other-active-turn-action-block" -> BattleAbilityEffect.EveryOtherActiveTurnActionBlock()
		"side-grass-major-status-immunity" -> BattleAbilityEffect.SideElementMajorStatusImmunity(
			elementIds.requiredElementId("grass"),
			BattleMajorStatus.entries.toSet(),
		)
		"side-grass-stat-drop-immunity" ->
			BattleAbilityEffect.SideElementStatDropImmunity(elementIds.requiredElementId("grass"))
		"switch-in-attack-drop-immunity" ->
			BattleAbilityEffect.SwitchInStatStageReductionImmunity(setOf(BattleStat.ATTACK))
		"switch-in-attack-drop-react-attack-plus-one" -> BattleAbilityEffect.SwitchInStatReductionReactiveBoost(
			triggerStat = BattleStat.ATTACK,
			boostStat = BattleStat.ATTACK,
			stageDelta = 1,
		)
		"secondary-effects-suppressed-damage-thirteen-tenths" ->
			BattleAbilityEffect.SecondaryEffectsSuppressedDamageBoost(1.3)
		"element-fire-absorb-damage-boost-one-and-half" -> BattleAbilityEffect.ElementSkillAbsorbDamageBoost(
			elementIds.requiredElementId("fire"),
			1.5,
		)
		"switch-in-copy-opponent-ability" -> BattleAbilityEffect.SwitchInCopyOpponentAbility()
		"contact-replace-attacker-ability-with-holder" ->
			BattleAbilityEffect.ContactReplaceAttackerAbilityWithHolder()
		"contact-swap-abilities" -> BattleAbilityEffect.ContactSwapAbilities()
		"fainted-ally-ability-copy" -> BattleAbilityEffect.FaintedAllyAbilityCopy()
		"opponent-major-status-reflection" -> BattleAbilityEffect.OpponentMajorStatusReflection()
		"opponent-targeted-status-skill-reflection" -> BattleAbilityEffect.OpponentStatusSkillReflection()
		"dance-move-copy" -> BattleAbilityEffect.DanceMoveCopy()
		"first-skill-element-change-since-switch-in" -> BattleAbilityEffect.FirstSkillElementChangeSinceSwitchIn()
		"single-target-second-hit-quarter-damage" -> BattleAbilityEffect.SingleTargetSecondHit()
		"poison-application-confusion" -> BattleAbilityEffect.PoisonApplicationConfusion()
		"terastallization-environment-clear" -> BattleAbilityEffect.TerastallizationEnvironmentClear()
		"opponent-stat-stage-increase-copy" -> BattleAbilityEffect.OpponentStatStageIncreaseCopy()
		"opponent-stat-stage-reduction-reflection" ->
			BattleAbilityEffect.OpponentStatStageReductionReflection()
		"low-hp-item-trigger-threshold-half" -> BattleAbilityEffect.LowHpItemTriggerThresholdHalf()
		"berry-consumption-heal-third" -> BattleAbilityEffect.BerryConsumptionHeal(3)
		"end-turn-consumed-berry-restore-half-sun-guaranteed" ->
			BattleAbilityEffect.EndTurnConsumedBerryRestore(50, BattleWeather.SUN)
		"end-turn-next-turn-consumed-berry-replay" -> BattleAbilityEffect.EndTurnConsumedBerryReplay(1)
		"end-turn-pickup-last-consumed-item" -> BattleAbilityEffect.EndTurnPickupConsumedItem()
		"berry-effect-double" -> BattleAbilityEffect.BerryEffectMultiplier(2.0)
		"opponent-berry-consumption-prevention" -> BattleAbilityEffect.OpponentBerryConsumptionPrevention()
		"ally-item-consumption-transfer" -> BattleAbilityEffect.AllyItemConsumptionTransfer()
		"sun-highest-stat-boost" ->
			BattleAbilityEffect.EnvironmentHighestStatMultiplier(requiredWeather = BattleWeather.SUN)
		"electric-terrain-highest-stat-boost" ->
			BattleAbilityEffect.EnvironmentHighestStatMultiplier(requiredTerrain = BattleTerrain.ELECTRIC)
		"element-electric-absorb-special-attack-up" -> BattleAbilityEffect.ElementSkillAbsorbStatStage(
			elementId = elementIds.requiredElementId("electric"),
			stat = BattleStat.SPECIAL_ATTACK,
			stageDelta = 1,
		)
		"element-water-absorb-special-attack-up" -> BattleAbilityEffect.ElementSkillAbsorbStatStage(
			elementId = elementIds.requiredElementId("water"),
			stat = BattleStat.SPECIAL_ATTACK,
			stageDelta = 1,
		)
		"contact-burn" -> BattleAbilityEffect.ContactStatusOnAttacker(BattleMajorStatus.BURN, 30)
		"contact-poison" -> BattleAbilityEffect.ContactStatusOnAttacker(BattleMajorStatus.POISON, 30)
		"contact-random-poison-paralysis-sleep" -> BattleAbilityEffect.RandomContactStatusOnAttacker(
			statuses = listOf(BattleMajorStatus.POISON, BattleMajorStatus.PARALYSIS, BattleMajorStatus.SLEEP),
			chancePercent = 30,
		)
		"contact-attacker-speed-minus-one" -> BattleAbilityEffect.ReceivedDamageStatStageChange(
			mapOf(BattleStat.SPEED to -1),
			requiresContact = true,
			changesAttacker = true,
		)
		"received-contact-shared-perish-countdown-three" ->
			BattleAbilityEffect.ContactSharedPerishCountdown(3)
		"received-damage-defense-plus-one" ->
			BattleAbilityEffect.ReceivedDamageStatStageChange(mapOf(BattleStat.DEFENSE to 1))
		"received-physical-defense-minus-one-speed-plus-two" -> BattleAbilityEffect.ReceivedDamageStatStageChange(
			mapOf(BattleStat.DEFENSE to -1, BattleStat.SPEED to 2),
			damageClasses = setOf(BattleDamageClass.PHYSICAL),
		)
		"received-water-defense-plus-two" -> BattleAbilityEffect.ReceivedDamageStatStageChange(
			mapOf(BattleStat.DEFENSE to 2),
			elementIds = setOf(elementIds.requiredElementId("water")),
		)
		"received-fire-water-speed-plus-six" -> BattleAbilityEffect.ReceivedDamageStatStageChange(
			mapOf(BattleStat.SPEED to 6),
			elementIds = setOf(elementIds.requiredElementId("fire"), elementIds.requiredElementId("water")),
		)
		"received-dark-attack-plus-one" -> BattleAbilityEffect.ReceivedDamageStatStageChange(
			mapOf(BattleStat.ATTACK to 1),
			elementIds = setOf(elementIds.requiredElementId("dark")),
		)
		"received-bug-dark-ghost-speed-plus-one" -> BattleAbilityEffect.ReceivedDamageStatStageChange(
			mapOf(BattleStat.SPEED to 1),
			elementIds = setOf(
				elementIds.requiredElementId("bug"),
				elementIds.requiredElementId("dark"),
				elementIds.requiredElementId("ghost"),
			),
		)
		"received-fire-attack-plus-one" -> BattleAbilityEffect.ReceivedDamageStatStageChange(
			mapOf(BattleStat.ATTACK to 1),
			elementIds = setOf(elementIds.requiredElementId("fire")),
		)
		"critical-damage-set-attack-plus-six" ->
			BattleAbilityEffect.CriticalDamageSetStatStage(BattleStat.ATTACK, 6)
		"cross-half-hp-special-attack-plus-one" -> BattleAbilityEffect.DamageCrossedHpThresholdStatStageChange(
			mapOf(BattleStat.SPECIAL_ATTACK to 1),
		)
		"cross-half-hp-anger-shell-stages" -> BattleAbilityEffect.DamageCrossedHpThresholdStatStageChange(
			mapOf(
				BattleStat.ATTACK to 1,
				BattleStat.SPECIAL_ATTACK to 1,
				BattleStat.SPEED to 1,
				BattleStat.DEFENSE to -1,
				BattleStat.SPECIAL_DEFENSE to -1,
			),
		)
		"received-damage-weather-sandstorm" ->
			BattleAbilityEffect.ReceivedDamageWeatherChange(BattleWeather.SANDSTORM)
		"received-damage-terrain-grassy" ->
			BattleAbilityEffect.ReceivedDamageTerrainChange(BattleTerrain.GRASSY)
		"element-dark-damage-boost-four-thirds" -> BattleAbilityEffect.FieldElementSkillDamageAura(
			elementIds.requiredElementId("dark"),
			4.0 / 3.0,
			3.0 / 4.0,
		)
		"element-fairy-damage-boost-four-thirds" -> BattleAbilityEffect.FieldElementSkillDamageAura(
			elementIds.requiredElementId("fairy"),
			4.0 / 3.0,
			3.0 / 4.0,
		)
		"field-damage-aura-reversal" -> BattleAbilityEffect.FieldDamageAuraReversal()
		"element-steel-damage-boost-one-and-half" -> BattleAbilityEffect.ElementSkillDamageBoost(
			setOf(elementIds.requiredElementId("steel")),
			1.5,
		)
		"element-water-damage-boost-double" -> BattleAbilityEffect.ElementSkillDamageBoost(
			setOf(elementIds.requiredElementId("water")),
			2.0,
		)
		"received-fire-ice-damage-half" -> BattleAbilityEffect.ElementSkillDamageReduction(
			setOf(elementIds.requiredElementId("fire"), elementIds.requiredElementId("ice")),
			0.5,
		)
		"received-fire-damage-half" -> BattleAbilityEffect.ElementSkillDamageReduction(
			setOf(elementIds.requiredElementId("fire")),
			0.5,
		)
		"received-fire-damage-double" -> BattleAbilityEffect.ElementSkillDamageReduction(
			setOf(elementIds.requiredElementId("fire")),
			2.0,
		)
		"received-ghost-damage-half" -> BattleAbilityEffect.ElementSkillDamageReduction(
			setOf(elementIds.requiredElementId("ghost")),
			0.5,
		)
		"super-effective-damage-boost-quarter" -> BattleAbilityEffect.EffectivenessDamageBoost(
			1.25,
			requiresSuperEffective = true,
		)
		"not-very-effective-damage-boost-double" -> BattleAbilityEffect.EffectivenessDamageBoost(
			2.0,
			requiresNotVeryEffective = true,
		)
		"critical-hit-damage-boost-one-and-half" -> BattleAbilityEffect.CriticalHitDamageBoost(1.5)
		"accuracy-multiplier-thirteen-tenths" -> BattleAbilityEffect.AccuracyMultiplier(1.3)
		"accuracy-multiplier-eleven-tenths" -> BattleAbilityEffect.AccuracyMultiplier(1.1)
		"physical-accuracy-multiplier-four-fifths" -> BattleAbilityEffect.AccuracyMultiplier(
			0.8,
			setOf(BattleDamageClass.PHYSICAL),
		)
		"opponent-accuracy-sandstorm-four-fifths" -> BattleAbilityEffect.OpponentAccuracyMultiplier(
			0.8,
			requiredWeather = BattleWeather.SANDSTORM,
		)
		"opponent-accuracy-snow-four-fifths" -> BattleAbilityEffect.OpponentAccuracyMultiplier(
			0.8,
			requiredWeather = BattleWeather.SNOW,
		)
		"opponent-accuracy-confusion-half" -> BattleAbilityEffect.OpponentAccuracyMultiplier(
			0.5,
			requiresConfusion = true,
		)
		"accuracy-always-hit" -> BattleAbilityEffect.AlwaysHit()
		"status-skill-accuracy-cap-half" -> BattleAbilityEffect.StatusSkillAccuracyCap(50)
		"powder-skill-immunity" -> BattleAbilityEffect.PowderSkillImmunity()
		"damaging-skill-secondary-effect-immunity" -> BattleAbilityEffect.DamagingSkillSecondaryEffectImmunity()
		"contact-suppression" -> BattleAbilityEffect.ContactSuppression()
		"critical-hit-stage-plus-one" -> BattleAbilityEffect.CriticalHitStageBoost(1)
		"multi-hit-maximum" -> BattleAbilityEffect.MultiHitMaximum()
		"base-power-at-most-sixty-damage-one-and-half" -> BattleAbilityEffect.BasePowerAtMostDamageBoost(60, 1.5)
		"recoil-skill-damage-six-fifths" -> BattleAbilityEffect.RecoilSkillDamageBoost(1.2)
		"switch-out-major-status-cure" -> BattleAbilityEffect.SwitchOutMajorStatusCure()
		"switch-out-heal-third" -> BattleAbilityEffect.SwitchOutHeal(3)
		"caused-faint-attack-plus-one" -> BattleAbilityEffect.FaintStatStageBoost(BattleStat.ATTACK, 1, true)
		"caused-faint-special-attack-plus-one" ->
			BattleAbilityEffect.FaintStatStageBoost(BattleStat.SPECIAL_ATTACK, 1, true)
		"any-faint-special-attack-plus-one" ->
			BattleAbilityEffect.FaintStatStageBoost(BattleStat.SPECIAL_ATTACK, 1, false)
		"caused-faint-highest-stat-plus-one" -> BattleAbilityEffect.FaintHighestStatBoost()
		"caused-faint-once-attack-special-attack-speed-plus-one" ->
			BattleAbilityEffect.OncePerBattleCausedFaintMultiStatBoost(
				setOf(BattleStat.ATTACK, BattleStat.SPECIAL_ATTACK, BattleStat.SPEED),
				1,
			)
		"end-turn-speed-plus-one" -> BattleAbilityEffect.EndTurnStatStageChange(BattleStat.SPEED, 1)
		"end-turn-random-stat-plus-two-minus-one" -> BattleAbilityEffect.EndTurnRandomStatStageChange(2, -1)
		"end-turn-major-status-cure-third" -> BattleAbilityEffect.EndTurnMajorStatusCure(33)
		"end-turn-major-status-cure-rain" -> BattleAbilityEffect.EndTurnMajorStatusCure(
			requiredWeathers = setOf(BattleWeather.RAIN),
		)
		"end-turn-ally-major-status-cure-thirty-percent" -> BattleAbilityEffect.EndTurnAllyMajorStatusCure(30)
		"poison-status-end-turn-heal-eighth" -> BattleAbilityEffect.MajorStatusEndTurnHeal(
			setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
			8,
		)
		"sun-end-turn-damage-eighth" -> BattleAbilityEffect.WeatherEndTurnDamage(setOf(BattleWeather.SUN), 8)
		"sleeping-opponents-end-turn-damage-eighth" -> BattleAbilityEffect.OpponentMajorStatusEndTurnDamage(
			setOf(BattleMajorStatus.SLEEP),
			8,
		)
		"major-status-immunity-paralysis" -> BattleAbilityEffect.MajorStatusImmunity(setOf(BattleMajorStatus.PARALYSIS))
		"major-status-immunity-sleep" -> BattleAbilityEffect.MajorStatusImmunity(setOf(BattleMajorStatus.SLEEP))
		"major-status-immunity-freeze" -> BattleAbilityEffect.MajorStatusImmunity(setOf(BattleMajorStatus.FREEZE))
		"always-treated-asleep-major-status-immunity" -> BattleAbilityEffect.AlwaysTreatedAsleep()
		"major-status-immunity-burn" -> BattleAbilityEffect.MajorStatusImmunity(setOf(BattleMajorStatus.BURN))
		"major-status-immunity-all" -> BattleAbilityEffect.MajorStatusImmunity(BattleMajorStatus.entries.toSet())
		"sun-major-status-immunity-all" -> BattleAbilityEffect.MajorStatusImmunity(
			BattleMajorStatus.entries.toSet(),
			BattleWeather.SUN,
		)
		"poison-element-status-bypass" -> BattleAbilityEffect.PoisonElementStatusBypass()
		"opponent-status-skill-immunity" -> BattleAbilityEffect.OpponentStatusSkillImmunity()
		"opponent-skill-pp-cost-plus-one" -> BattleAbilityEffect.OpponentSkillPpCostIncrease(1)
		"grounded-opponent-switch-restriction" -> BattleAbilityEffect.OpponentSwitchRestriction(
			requiresGroundedTarget = true,
		)
		"opponent-switch-restriction-same-effect-immunity" -> BattleAbilityEffect.OpponentSwitchRestriction(
			sameEffectGrantsImmunity = true,
		)
		"steel-opponent-switch-restriction" -> BattleAbilityEffect.OpponentSwitchRestriction(
			requiredTargetElementId = elementIds.requiredElementId("steel"),
		)
		"forced-switch-immunity" -> BattleAbilityEffect.ForcedSwitchImmunity()
		"normal-fighting-type-immunity-bypass" -> BattleAbilityEffect.ElementTypeImmunityBypass(
			setOf(elementIds.requiredElementId("normal"), elementIds.requiredElementId("fighting")),
		)
		"non-super-effective-damage-immunity" -> BattleAbilityEffect.NonSuperEffectiveDamageImmunity()
		"contact-faint-attacker-max-hp-quarter-damage" -> BattleAbilityEffect.FaintAttackerDamage(
			requiresContact = true,
			attackerMaxHpDenominator = 4,
			suppressedByExplosionSuppression = true,
		)
		"faint-attacker-damage-taken" -> BattleAbilityEffect.FaintAttackerDamage(usesDamageTaken = true)
		"cross-half-hp-force-self-switch" -> BattleAbilityEffect.DamageCrossedHpThresholdForceSelfSwitch()
		"received-damage-all-other-speed-minus-one" ->
			BattleAbilityEffect.ReceivedDamageAllOtherStatStageChange(BattleStat.SPEED, -1)
		"received-damage-disable-attacker-skill-thirty-percent" ->
			BattleAbilityEffect.ReceivedDamageDisableAttackerSkill(30, 4)
		"flinch-speed-plus-one" -> BattleAbilityEffect.FlinchStatStageBoost(BattleStat.SPEED, 1)
		"item-lost-speed-double" -> BattleAbilityEffect.ItemLostSpeedMultiplier(2.0)
		"sleep-duration-half" -> BattleAbilityEffect.SleepDurationDivisor(2)
		"side-major-status-immunity-sleep" ->
			BattleAbilityEffect.SideMajorStatusImmunity(setOf(BattleMajorStatus.SLEEP))
		"side-major-status-immunity-poison" -> BattleAbilityEffect.SideMajorStatusImmunity(
			setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
		)
		"poisoned-target-guaranteed-critical-hit" -> BattleAbilityEffect.MajorStatusGuaranteedCriticalHit(
			setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
		)
		"dealt-contact-damage-poison-thirty-percent" -> BattleAbilityEffect.DealtDamageMajorStatusChance(
			BattleMajorStatus.POISON,
			30,
			requiresContact = true,
		)
		"dealt-damage-poison-thirty-percent" -> BattleAbilityEffect.DealtDamageMajorStatusChance(
			BattleMajorStatus.POISON,
			30,
		)
		"volatile-status-immunity-confusion" ->
			BattleAbilityEffect.VolatileStatusImmunity(setOf(io.github.lishangbu.battleengine.model.BattleVolatileStatus.CONFUSION))
		"volatile-status-immunity-flinch" ->
			BattleAbilityEffect.VolatileStatusImmunity(setOf(io.github.lishangbu.battleengine.model.BattleVolatileStatus.FLINCH))
		"volatile-status-immunity-infatuation-taunt" -> BattleAbilityEffect.VolatileStatusImmunity(
			setOf(
				io.github.lishangbu.battleengine.model.BattleVolatileStatus.INFATUATION,
				io.github.lishangbu.battleengine.model.BattleVolatileStatus.TAUNT,
			),
		)
		"attack-stat-one-and-half" -> BattleAbilityEffect.AttackingStatMultiplier(BattleStat.ATTACK, 1.5)
		"burn-special-attack-stat-one-and-half" -> BattleAbilityEffect.AttackingStatMultiplier(
			BattleStat.SPECIAL_ATTACK,
			1.5,
			requiredMajorStatuses = setOf(BattleMajorStatus.BURN),
		)
		"poison-attack-stat-one-and-half" -> BattleAbilityEffect.AttackingStatMultiplier(
			BattleStat.ATTACK,
			1.5,
			requiredMajorStatuses = setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
		)
		"major-status-defense-stat-one-and-half" -> BattleAbilityEffect.DefendingStatMultiplier(
			BattleStat.DEFENSE,
			1.5,
			requiresMajorStatus = true,
		)
		"opponent-attack-stat-three-quarters" ->
			BattleAbilityEffect.OpponentAttackingStatMultiplier(BattleStat.ATTACK, 0.75)
		"opponent-special-attack-stat-three-quarters" ->
			BattleAbilityEffect.OpponentAttackingStatMultiplier(BattleStat.SPECIAL_ATTACK, 0.75)
		"opponent-defense-stat-three-quarters" ->
			BattleAbilityEffect.OpponentDefendingStatMultiplier(BattleStat.DEFENSE, 0.75)
		"opponent-special-defense-stat-three-quarters" ->
			BattleAbilityEffect.OpponentDefendingStatMultiplier(BattleStat.SPECIAL_DEFENSE, 0.75)
		"received-contact-damage-half" -> BattleAbilityEffect.ContactBasedSkillDamageReduction(0.5)
		"major-status-speed-one-and-half-ignore-paralysis" ->
			BattleAbilityEffect.MajorStatusSpeedMultiplier(1.5, ignoresParalysisReduction = true)
		"sun-special-attack-stat-one-and-half" -> BattleAbilityEffect.AttackingStatMultiplier(
			BattleStat.SPECIAL_ATTACK,
			1.5,
			requiredWeather = BattleWeather.SUN,
		)
		"sun-attack-stat-four-thirds" -> BattleAbilityEffect.AttackingStatMultiplier(
			BattleStat.ATTACK,
			4.0 / 3.0,
			requiredWeather = BattleWeather.SUN,
		)
		"electric-terrain-special-attack-stat-four-thirds" -> BattleAbilityEffect.AttackingStatMultiplier(
			BattleStat.SPECIAL_ATTACK,
			4.0 / 3.0,
			requiredTerrain = BattleTerrain.ELECTRIC,
		)
		"half-hp-attack-stat-half" -> BattleAbilityEffect.AttackingStatMultiplier(
			BattleStat.ATTACK,
			0.5,
			maximumHpFraction = 0.5,
		)
		"half-hp-special-attack-stat-half" -> BattleAbilityEffect.AttackingStatMultiplier(
			BattleStat.SPECIAL_ATTACK,
			0.5,
			maximumHpFraction = 0.5,
		)
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
	this in ABILITY_POLICIES_WITHOUT_EFFECT_OBJECT || toBattleAbilityEffect(elementIds) != null

/**
 * 已由运行时承载、但不需要创建 [BattleAbilityEffect] 的特性策略。
 *
 * `ground-immunity` 会写入成员接地事实；`single-battle-no-effect` 用于只在捕获、逃跑或双打目标选择中生效，
 * 因而在当前第九世代 National Dex 单打赛制内没有战斗 hook 的特性。
 */
internal val ABILITY_POLICIES_WITHOUT_EFFECT_OBJECT = setOf(
	"ground-immunity",
	"single-battle-no-effect",
)
