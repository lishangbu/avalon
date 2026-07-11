package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.random.BattleRandom

internal fun sessionInitialState(
	first: BattleParticipant = sessionParticipant("side-1-actor-1", speed = 100),
	second: BattleParticipant = sessionParticipant("side-2-actor-1", speed = 80),
	firstBench: List<BattleParticipant> = emptyList(),
	secondBench: List<BattleParticipant> = emptyList(),
	maxTurns: Int? = null,
	formatCode: String = "standard-single",
): BattleInitialState =
	BattleInitialState(
		format = BattleFormatSnapshot(
			code = formatCode,
			mode = BattleMode.SINGLE,
			activeParticipantsPerSide = 1,
			maxTurns = maxTurns,
		),
		rules = BattleRuleSnapshot(),
		sides = listOf(
			BattleSide(
				sideId = "side-1",
				activeActorIds = listOf(first.actorId),
				participants = listOf(first) + firstBench,
			),
			BattleSide(
				sideId = "side-2",
				activeActorIds = listOf(second.actorId),
				participants = listOf(second) + secondBench,
			),
		),
	)

internal fun sessionParticipant(
	actorId: String,
	speed: Int,
	currentHp: Int = 100,
	skill: BattleSkillSlot = sessionSkill(),
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
		weight = 1000,
		elementIds = setOf(1),
		skillSlots = listOf(skill),
	)

internal fun sessionSkill(
	skillId: Long = 1,
	remainingPp: Int = 35,
): BattleSkillSlot =
	BattleSkillSlot(
		skillId = skillId,
		name = "撞击",
		elementId = 1,
		damageClass = BattleDamageClass.PHYSICAL,
		power = 40,
		accuracy = null,
		remainingPp = remainingPp,
		maxPp = 35,
	)

internal fun zeroBattleRandom(): BattleRandom =
	object : BattleRandom {
		override fun nextInt(bound: Int, reason: String): Int = 0
	}
