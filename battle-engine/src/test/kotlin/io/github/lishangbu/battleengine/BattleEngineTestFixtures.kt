package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderApplication
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleOneHitKnockOut
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleProportionalDamage
import io.github.lishangbu.battleengine.model.BattleReceivedDamage
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSideConditionApplication
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardApplication
import io.github.lishangbu.battleengine.model.BattleSideProtection
import io.github.lishangbu.battleengine.model.BattleSideProtectionApplication
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierApplication
import io.github.lishangbu.battleengine.model.BattleSkillDynamicPower
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleSkillPostDamageStatusCure
import io.github.lishangbu.battleengine.model.BattleSkillPowerMultiplier
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleSkillWeightEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart

internal fun singleFormat(teamSize: Int? = null): BattleFormatSnapshot =
	BattleFormatSnapshot(
		code = "standard-single",
		mode = BattleMode.SINGLE,
		activeParticipantsPerSide = 1,
		teamSize = teamSize,
	)

internal fun doubleFormat(teamSize: Int? = null): BattleFormatSnapshot =
	BattleFormatSnapshot(
		code = "standard-double",
		mode = BattleMode.DOUBLE,
		activeParticipantsPerSide = 2,
		teamSize = teamSize,
	)

internal fun neutralRules(): BattleRuleSnapshot =
	BattleRuleSnapshot(
		elementChart = ElementEffectivenessChart.neutral(),
		elementIds = mapOf(
			"dark" to 17,
			"electric" to 13,
			"fire" to 10,
			"grass" to 12,
			"ground" to 5,
			"ice" to 15,
			"poison" to 4,
			"psychic" to 14,
			"rock" to 6,
			"steel" to 9,
			"water" to 11,
			"dragon" to 16,
		),
	)

internal fun initialState(
	first: BattleParticipant = participant("side-a-active", speed = 100),
	second: BattleParticipant = participant("side-b-active", speed = 80),
	firstBench: List<BattleParticipant> = emptyList(),
	secondBench: List<BattleParticipant> = emptyList(),
	rules: BattleRuleSnapshot = neutralRules(),
	environment: BattleEnvironment = BattleEnvironment(),
	firstSideDamageReductions: List<BattleSideDamageReduction> = emptyList(),
	secondSideDamageReductions: List<BattleSideDamageReduction> = emptyList(),
	firstSideEntryHazards: List<BattleSideEntryHazard> = emptyList(),
	secondSideEntryHazards: List<BattleSideEntryHazard> = emptyList(),
	firstSideProtections: List<BattleSideProtection> = emptyList(),
	secondSideProtections: List<BattleSideProtection> = emptyList(),
): BattleInitialState =
	BattleInitialState(
		format = singleFormat(),
		rules = rules,
		environment = environment,
		sides = listOf(
			BattleSide(
				sideId = "side-a",
				activeActorIds = listOf(first.actorId),
				participants = listOf(first) + firstBench,
				damageReductions = firstSideDamageReductions,
				entryHazards = firstSideEntryHazards,
				protections = firstSideProtections,
			),
			BattleSide(
				sideId = "side-b",
				activeActorIds = listOf(second.actorId),
				participants = listOf(second) + secondBench,
				damageReductions = secondSideDamageReductions,
				entryHazards = secondSideEntryHazards,
				protections = secondSideProtections,
			),
		),
	)

internal fun doubleInitialState(
	firstA: BattleParticipant = participant("side-a-active-1", speed = 100),
	firstB: BattleParticipant = participant("side-a-active-2", speed = 90),
	secondA: BattleParticipant = participant("side-b-active-1", speed = 80),
	secondB: BattleParticipant = participant("side-b-active-2", speed = 70),
	rules: BattleRuleSnapshot = neutralRules(),
	environment: BattleEnvironment = BattleEnvironment(),
	firstSideDamageReductions: List<BattleSideDamageReduction> = emptyList(),
	secondSideDamageReductions: List<BattleSideDamageReduction> = emptyList(),
	firstSideProtections: List<BattleSideProtection> = emptyList(),
	secondSideProtections: List<BattleSideProtection> = emptyList(),
): BattleInitialState =
	BattleInitialState(
		format = doubleFormat(),
		rules = rules,
		environment = environment,
		sides = listOf(
			BattleSide(
				sideId = "side-a",
				activeActorIds = listOf(firstA.actorId, firstB.actorId),
				participants = listOf(firstA, firstB),
				damageReductions = firstSideDamageReductions,
				protections = firstSideProtections,
			),
			BattleSide(
				sideId = "side-b",
				activeActorIds = listOf(secondA.actorId, secondB.actorId),
				participants = listOf(secondA, secondB),
				damageReductions = secondSideDamageReductions,
				protections = secondSideProtections,
			),
		),
	)

internal fun participant(
	actorId: String,
	speed: Int,
	currentHp: Int = 100,
	level: Int = 50,
	elementId: Long = 1,
	weight: Int = 1000,
	weightReduction: Int = 0,
	skill: BattleSkillSlot = damagingSkill(),
	abilityId: Long? = null,
	itemId: Long? = null,
	grounded: Boolean = true,
	criticalHitStageBonus: Int = 0,
	abilityEffects: List<BattleAbilityEffect> = emptyList(),
	itemEffects: List<BattleItemEffect> = emptyList(),
): BattleParticipant =
	BattleParticipant(
		actorId = actorId,
		creatureId = 1,
		level = level,
		maxHp = 100,
		currentHp = currentHp,
		attack = 100,
		defense = 100,
		specialAttack = 100,
		specialDefense = 100,
		speed = speed,
		weight = weight,
		weightReduction = weightReduction,
		elementIds = setOf(elementId),
		skillSlots = listOf(skill),
		abilityId = abilityId,
		itemId = itemId,
		grounded = grounded,
		criticalHitStageBonus = criticalHitStageBonus,
		abilityEffects = abilityEffects,
		itemEffects = itemEffects,
	)

internal fun damagingSkill(
	skillId: Long = 1,
	name: String = "撞击",
	elementId: Long = 1,
	damageClass: BattleDamageClass = BattleDamageClass.PHYSICAL,
	power: Int? = 40,
	fixedDamage: BattleFixedDamage? = null,
	proportionalDamage: BattleProportionalDamage? = null,
	hpDerivedDamage: BattleHpDerivedDamage? = null,
	receivedDamage: BattleReceivedDamage? = null,
	oneHitKnockOut: BattleOneHitKnockOut? = null,
	accuracy: Int? = null,
	targetScope: BattleSkillTargetScope = BattleSkillTargetScope.SELECTED_TARGET,
	minHits: Int = 1,
	maxHits: Int = 1,
	makesContact: Boolean = false,
	criticalHitStage: Int = 0,
	criticalHitStageBoost: Int = 0,
	affectedByProtect: Boolean = true,
	protectsUser: Boolean = false,
	enduresFatalDamage: Boolean = false,
	thawsUserBeforeMove: Boolean = false,
	soundBased: Boolean = false,
	powderBased: Boolean = false,
	punchBased: Boolean = false,
	slicingBased: Boolean = false,
	weakenedByGrassyTerrain: Boolean = false,
	chargesBeforeUse: Boolean = false,
	chargeSkippedByWeathers: Set<BattleWeather> = emptySet(),
	rechargesAfterUse: Boolean = false,
	accuracyOverridesByWeather: Map<BattleWeather, Int?> = emptyMap(),
	powerMultipliersByWeather: Map<BattleWeather, Double> = emptyMap(),
	groundedPowerMultipliersByTerrain: Map<BattleTerrain, Double> = emptyMap(),
	conditionalPowerMultipliers: List<BattleSkillPowerMultiplier> = emptyList(),
	dynamicPower: BattleSkillDynamicPower? = null,
	defendingStatOverride: BattleStat? = null,
	leavesTargetAtOneHp: Boolean = false,
	breaksTargetSideDamageReductions: Boolean = false,
	typelessDamage: Boolean = false,
	elementOverridesByWeather: Map<BattleWeather, Long> = emptyMap(),
	elementOverridesByTerrain: Map<BattleTerrain, Long> = emptyMap(),
	ignoresUserBurnAttackReduction: Boolean = false,
	lockMoveTurnsMin: Int = 1,
	lockMoveTurnsMax: Int = 1,
	confusesUserAfterLock: Boolean = false,
	forceTargetSwitch: Boolean = false,
	locksAccuracyOnTarget: Boolean = false,
	targetLastSkillPpReduction: Int = 0,
	plantsLeechSeed: Boolean = false,
	clearsUserSideHazardsAndTraps: Boolean = false,
	priority: Int = 0,
	groundedTerrainPriorityBoosts: Map<BattleTerrain, Int> = emptyMap(),
	statusApplications: List<BattleStatusApplication> = emptyList(),
	volatileStatusApplications: List<BattleVolatileStatusApplication> = emptyList(),
	statStageEffects: List<BattleStatStageEffect> = emptyList(),
	statStageOperations: List<BattleStatStageOperation> = emptyList(),
	sideConditionApplications: List<BattleSideConditionApplication> = emptyList(),
	sideSpeedModifierApplications: List<BattleSideSpeedModifierApplication> = emptyList(),
	sideEntryHazardApplications: List<BattleSideEntryHazardApplication> = emptyList(),
	sideProtectionApplications: List<BattleSideProtectionApplication> = emptyList(),
	curesUserSideMajorStatuses: Boolean = false,
	curesUserMajorStatus: Boolean = false,
	curesUserSideActiveMajorStatuses: Boolean = false,
	fieldSpeedOrderApplications: List<BattleFieldSpeedOrderApplication> = emptyList(),
	hpEffects: List<BattleSkillHpEffect> = emptyList(),
	restoresUserBySleeping: Boolean = false,
	postDamageStatusCures: List<BattleSkillPostDamageStatusCure> = emptyList(),
	removesUserElementAfterDamage: Boolean = false,
	weightEffects: List<BattleSkillWeightEffect> = emptyList(),
	environmentEffects: List<BattleSkillEnvironmentEffect> = emptyList(),
): BattleSkillSlot =
	BattleSkillSlot(
		skillId = skillId,
		name = name,
		elementId = elementId,
		damageClass = damageClass,
		power = power,
		fixedDamage = fixedDamage,
		proportionalDamage = proportionalDamage,
		hpDerivedDamage = hpDerivedDamage,
		receivedDamage = receivedDamage,
		oneHitKnockOut = oneHitKnockOut,
		accuracy = accuracy,
		targetScope = targetScope,
		minHits = minHits,
		maxHits = maxHits,
		makesContact = makesContact,
		criticalHitStage = criticalHitStage,
		criticalHitStageBoost = criticalHitStageBoost,
		affectedByProtect = affectedByProtect,
		protectsUser = protectsUser,
		enduresFatalDamage = enduresFatalDamage,
		thawsUserBeforeMove = thawsUserBeforeMove,
		soundBased = soundBased,
		powderBased = powderBased,
		punchBased = punchBased,
		slicingBased = slicingBased,
		weakenedByGrassyTerrain = weakenedByGrassyTerrain,
		chargesBeforeUse = chargesBeforeUse,
		chargeSkippedByWeathers = chargeSkippedByWeathers,
		rechargesAfterUse = rechargesAfterUse,
		accuracyOverridesByWeather = accuracyOverridesByWeather,
		powerMultipliersByWeather = powerMultipliersByWeather,
		groundedPowerMultipliersByTerrain = groundedPowerMultipliersByTerrain,
		conditionalPowerMultipliers = conditionalPowerMultipliers,
		dynamicPower = dynamicPower,
		defendingStatOverride = defendingStatOverride,
		leavesTargetAtOneHp = leavesTargetAtOneHp,
		breaksTargetSideDamageReductions = breaksTargetSideDamageReductions,
		typelessDamage = typelessDamage,
		elementOverridesByWeather = elementOverridesByWeather,
		elementOverridesByTerrain = elementOverridesByTerrain,
		ignoresUserBurnAttackReduction = ignoresUserBurnAttackReduction,
		lockMoveTurnsMin = lockMoveTurnsMin,
		lockMoveTurnsMax = lockMoveTurnsMax,
		confusesUserAfterLock = confusesUserAfterLock,
		forceTargetSwitch = forceTargetSwitch,
		locksAccuracyOnTarget = locksAccuracyOnTarget,
		targetLastSkillPpReduction = targetLastSkillPpReduction,
		plantsLeechSeed = plantsLeechSeed,
		clearsUserSideHazardsAndTraps = clearsUserSideHazardsAndTraps,
		priority = priority,
		groundedTerrainPriorityBoosts = groundedTerrainPriorityBoosts,
		remainingPp = 35,
		maxPp = 35,
		statusApplications = statusApplications,
		volatileStatusApplications = volatileStatusApplications,
		statStageEffects = statStageEffects,
		statStageOperations = statStageOperations,
		sideConditionApplications = sideConditionApplications,
		sideSpeedModifierApplications = sideSpeedModifierApplications,
		sideEntryHazardApplications = sideEntryHazardApplications,
		sideProtectionApplications = sideProtectionApplications,
		curesUserSideMajorStatuses = curesUserSideMajorStatuses,
		curesUserMajorStatus = curesUserMajorStatus,
		curesUserSideActiveMajorStatuses = curesUserSideActiveMajorStatuses,
		fieldSpeedOrderApplications = fieldSpeedOrderApplications,
		hpEffects = hpEffects,
		restoresUserBySleeping = restoresUserBySleeping,
		postDamageStatusCures = postDamageStatusCures,
		removesUserElementAfterDamage = removesUserElementAfterDamage,
		weightEffects = weightEffects,
		environmentEffects = environmentEffects,
	)

internal fun protectionSkill(
	skillId: Long = 2,
	name: String = "防护",
): BattleSkillSlot =
	damagingSkill(
		skillId = skillId,
		name = name,
		damageClass = BattleDamageClass.STATUS,
		power = null,
		affectedByProtect = false,
		protectsUser = true,
		priority = 4,
	).copy(remainingPp = 10, maxPp = 10)

internal fun endureSkill(
	skillId: Long = 203,
	name: String = "挺住",
): BattleSkillSlot =
	damagingSkill(
		skillId = skillId,
		name = name,
		damageClass = BattleDamageClass.STATUS,
		power = null,
		targetScope = BattleSkillTargetScope.SELF,
		affectedByProtect = false,
		enduresFatalDamage = true,
		priority = 4,
	).copy(remainingPp = 10, maxPp = 10)
