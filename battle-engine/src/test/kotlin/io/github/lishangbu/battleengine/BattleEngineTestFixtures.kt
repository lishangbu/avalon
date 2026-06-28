package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatusApplication
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
		electricElementId = 13,
		fireElementId = 10,
		grassElementId = 12,
		groundElementId = 5,
		iceElementId = 15,
		poisonElementId = 4,
		rockElementId = 6,
		steelElementId = 9,
		waterElementId = 11,
	)

internal fun initialState(
	first: BattleParticipant = participant("side-a-active", speed = 100),
	second: BattleParticipant = participant("side-b-active", speed = 80),
	firstBench: List<BattleParticipant> = emptyList(),
	secondBench: List<BattleParticipant> = emptyList(),
	rules: BattleRuleSnapshot = neutralRules(),
	environment: BattleEnvironment = BattleEnvironment(),
): BattleInitialState =
	BattleInitialState(
		format = singleFormat(),
		rules = rules,
		environment = environment,
		sides = listOf(
			BattleSide("side-a", listOf(first.actorId), listOf(first) + firstBench),
			BattleSide("side-b", listOf(second.actorId), listOf(second) + secondBench),
		),
	)

internal fun doubleInitialState(
	firstA: BattleParticipant = participant("side-a-active-1", speed = 100),
	firstB: BattleParticipant = participant("side-a-active-2", speed = 90),
	secondA: BattleParticipant = participant("side-b-active-1", speed = 80),
	secondB: BattleParticipant = participant("side-b-active-2", speed = 70),
	rules: BattleRuleSnapshot = neutralRules(),
	environment: BattleEnvironment = BattleEnvironment(),
): BattleInitialState =
	BattleInitialState(
		format = doubleFormat(),
		rules = rules,
		environment = environment,
		sides = listOf(
			BattleSide("side-a", listOf(firstA.actorId, firstB.actorId), listOf(firstA, firstB)),
			BattleSide("side-b", listOf(secondA.actorId, secondB.actorId), listOf(secondA, secondB)),
		),
	)

internal fun participant(
	actorId: String,
	speed: Int,
	currentHp: Int = 100,
	elementId: Long = 1,
	skill: BattleSkillSlot = damagingSkill(),
	abilityId: Long? = null,
	itemId: Long? = null,
	grounded: Boolean = true,
	abilityEffects: List<BattleAbilityEffect> = emptyList(),
	itemEffects: List<BattleItemEffect> = emptyList(),
): BattleParticipant =
	BattleParticipant(
		actorId = actorId,
		creatureId = 1,
		level = 50,
		maxHp = 100,
		currentHp = currentHp,
		attack = 100,
		defense = 100,
		specialAttack = 100,
		specialDefense = 100,
		speed = speed,
		elementIds = setOf(elementId),
		skillSlots = listOf(skill),
		abilityId = abilityId,
		itemId = itemId,
		grounded = grounded,
		abilityEffects = abilityEffects,
		itemEffects = itemEffects,
	)

internal fun damagingSkill(
	skillId: Long = 1,
	name: String = "撞击",
	elementId: Long = 1,
	damageClass: BattleDamageClass = BattleDamageClass.PHYSICAL,
	power: Int? = 40,
	accuracy: Int? = null,
	targetScope: BattleSkillTargetScope = BattleSkillTargetScope.SELECTED_TARGET,
	minHits: Int = 1,
	maxHits: Int = 1,
	makesContact: Boolean = false,
	criticalHitStage: Int = 0,
	affectedByProtect: Boolean = true,
	protectsUser: Boolean = false,
	thawsUserBeforeMove: Boolean = false,
	powderBased: Boolean = false,
	weakenedByGrassyTerrain: Boolean = false,
	accuracyOverridesByWeather: Map<BattleWeather, Int?> = emptyMap(),
	powerMultipliersByWeather: Map<BattleWeather, Double> = emptyMap(),
	lockMoveTurnsMin: Int = 1,
	lockMoveTurnsMax: Int = 1,
	confusesUserAfterLock: Boolean = false,
	priority: Int = 0,
	statusApplications: List<BattleStatusApplication> = emptyList(),
	volatileStatusApplications: List<BattleVolatileStatusApplication> = emptyList(),
	statStageEffects: List<BattleStatStageEffect> = emptyList(),
): BattleSkillSlot =
	BattleSkillSlot(
		skillId = skillId,
		name = name,
		elementId = elementId,
		damageClass = damageClass,
		power = power,
		accuracy = accuracy,
		targetScope = targetScope,
		minHits = minHits,
		maxHits = maxHits,
		makesContact = makesContact,
		criticalHitStage = criticalHitStage,
		affectedByProtect = affectedByProtect,
		protectsUser = protectsUser,
		thawsUserBeforeMove = thawsUserBeforeMove,
		powderBased = powderBased,
		weakenedByGrassyTerrain = weakenedByGrassyTerrain,
		accuracyOverridesByWeather = accuracyOverridesByWeather,
		powerMultipliersByWeather = powerMultipliersByWeather,
		lockMoveTurnsMin = lockMoveTurnsMin,
		lockMoveTurnsMax = lockMoveTurnsMax,
		confusesUserAfterLock = confusesUserAfterLock,
		priority = priority,
		remainingPp = 35,
		maxPp = 35,
		statusApplications = statusApplications,
		volatileStatusApplications = volatileStatusApplications,
		statStageEffects = statStageEffects,
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
