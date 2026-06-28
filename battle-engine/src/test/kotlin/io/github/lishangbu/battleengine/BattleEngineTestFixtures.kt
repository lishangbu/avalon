package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSkillSlot
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
	rules: BattleRuleSnapshot = neutralRules(),
): BattleInitialState =
	BattleInitialState(
		format = singleFormat(),
		rules = rules,
		sides = listOf(
			BattleSide("side-a", listOf(first.actorId), listOf(first)),
			BattleSide("side-b", listOf(second.actorId), listOf(second)),
		),
	)

internal fun participant(
	actorId: String,
	speed: Int,
	currentHp: Int = 100,
	elementId: Long = 1,
	skill: BattleSkillSlot = damagingSkill(),
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
	)

internal fun damagingSkill(
	skillId: Long = 1,
	name: String = "撞击",
	elementId: Long = 1,
	damageClass: BattleDamageClass = BattleDamageClass.PHYSICAL,
	power: Int = 40,
	accuracy: Int? = null,
	priority: Int = 0,
): BattleSkillSlot =
	BattleSkillSlot(
		skillId = skillId,
		name = name,
		elementId = elementId,
		damageClass = damageClass,
		power = power,
		accuracy = accuracy,
		priority = priority,
		remainingPp = 35,
		maxPp = 35,
	)
