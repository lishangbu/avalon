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
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart

internal fun singleFormat(): BattleFormatSnapshot =
	BattleFormatSnapshot(
		code = "standard-single",
		mode = BattleMode.SINGLE,
		activeParticipantsPerSide = 1,
	)

internal fun neutralRules(): BattleRuleSnapshot =
	BattleRuleSnapshot(elementChart = ElementEffectivenessChart.neutral())

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

internal fun participant(
	actorId: String,
	speed: Int,
	currentHp: Int = 100,
	elementId: Long = 1,
	skill: BattleSkillSlot = damagingSkill(),
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
	makesContact: Boolean = false,
	criticalHitStage: Int = 0,
	affectedByProtect: Boolean = true,
	protectsUser: Boolean = false,
	priority: Int = 0,
	statusApplications: List<BattleStatusApplication> = emptyList(),
	statStageEffects: List<BattleStatStageEffect> = emptyList(),
): BattleSkillSlot =
	BattleSkillSlot(
		skillId = skillId,
		name = name,
		elementId = elementId,
		damageClass = damageClass,
		power = power,
		accuracy = accuracy,
		makesContact = makesContact,
		criticalHitStage = criticalHitStage,
		affectedByProtect = affectedByProtect,
		protectsUser = protectsUser,
		priority = priority,
		remainingPp = 35,
		maxPp = 35,
		statusApplications = statusApplications,
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
