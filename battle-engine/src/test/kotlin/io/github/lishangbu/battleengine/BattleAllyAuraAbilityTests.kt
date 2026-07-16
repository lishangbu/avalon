package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleAllyAuraAbilityTests {
	@Test
	fun `battery power spot and friend guard modify only their active allies`() {
		val specialSkill = damagingSkill(skillId = 901, power = 80, damageClass = BattleDamageClass.SPECIAL)
		val neutral = damageWithAlly(specialSkill)
		val battery = damageWithAlly(
			specialSkill,
			allyEffects = listOf(BattleAbilityEffect.AllySkillDamageBoost(1.3, setOf(BattleDamageClass.SPECIAL))),
		)
		val powerSpot = damageWithAlly(
			specialSkill,
			allyEffects = listOf(BattleAbilityEffect.AllySkillDamageBoost(1.3)),
		)
		val friendGuard = damageWithAlly(
			specialSkill,
			defenderAllyEffects = listOf(BattleAbilityEffect.AllyReceivedDamageReduction(0.75)),
		)

		assertTrue(battery > neutral)
		assertEquals(battery, powerSpot)
		assertTrue(friendGuard < neutral)
	}

	@Test
	fun `plus and minus boost special attack when an active ally belongs to the same group`() {
		val specialSkill = damagingSkill(skillId = 902, power = 80, damageClass = BattleDamageClass.SPECIAL)
		val neutral = damageWithAlly(specialSkill)
		val paired = damageWithAlly(
			specialSkill,
			attackerEffects = listOf(
				BattleAbilityEffect.AllyAbilityPresenceAttackingStatMultiplier(
					groupCode = "plus-minus",
					stat = BattleStat.SPECIAL_ATTACK,
					multiplier = 1.5,
				),
			),
			allyEffects = listOf(
				BattleAbilityEffect.AllyAbilityGroupMembership("plus-minus"),
			),
		)

		assertTrue(paired > neutral)
	}

	@Test
	fun `telepathy blocks damaging skills from allies without blocking opponents`() {
		val skill = damagingSkill(skillId = 903, power = 80)
		val allyTarget = resolve(
			firstA = participant("attacker", 100, skill = skill),
			firstB = participant(
				"telepathy-target",
				90,
				abilityEffects = listOf(BattleAbilityEffect.AllyDamageImmunity()),
			),
			secondA = participant("opponent-a", 80),
			secondB = participant("opponent-b", 70),
			actorId = "attacker",
			targetActorId = "telepathy-target",
			skillId = skill.skillId,
		)
		val opponentTarget = resolve(
			firstA = participant("attacker", 100, skill = skill),
			firstB = participant("ally", 90),
			secondA = participant(
				"telepathy-target",
				80,
				abilityEffects = listOf(BattleAbilityEffect.AllyDamageImmunity()),
			),
			secondB = participant("opponent-b", 70),
			actorId = "attacker",
			targetActorId = "telepathy-target",
			skillId = skill.skillId,
		)

		assertEquals(100, allyTarget.participant("telepathy-target")?.currentHp)
		assertEquals(1, allyTarget.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>().size)
		assertTrue(requireNotNull(opponentTarget.participant("telepathy-target")).currentHp < 100)
	}

	private fun damageWithAlly(
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
		attackerEffects: List<BattleAbilityEffect> = emptyList(),
		allyEffects: List<BattleAbilityEffect> = emptyList(),
		defenderAllyEffects: List<BattleAbilityEffect> = emptyList(),
	): Int {
		val resolved = resolve(
			firstA = participant("attacker", 100, skill = skill, abilityEffects = attackerEffects),
			firstB = participant("attacker-ally", 90, abilityEffects = allyEffects),
			secondA = participant("defender", 80),
			secondB = participant("defender-ally", 70, abilityEffects = defenderAllyEffects),
			actorId = "attacker",
			targetActorId = "defender",
			skillId = skill.skillId,
		)
		return resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount
	}

	private fun resolve(
		firstA: io.github.lishangbu.battleengine.model.BattleParticipant,
		firstB: io.github.lishangbu.battleengine.model.BattleParticipant,
		secondA: io.github.lishangbu.battleengine.model.BattleParticipant,
		secondB: io.github.lishangbu.battleengine.model.BattleParticipant,
		actorId: String,
		targetActorId: String,
		skillId: Long,
	): io.github.lishangbu.battleengine.model.BattleState {
		val engine = BattleEngine()
		return engine.resolveTurn(
			engine.start(doubleInitialState(firstA, firstB, secondA, secondB)),
			listOf(BattleAction.UseSkill(actorId, skillId, targetActorId)),
			ScriptedBattleRandom(listOf(1, 15)),
		)
	}
}
