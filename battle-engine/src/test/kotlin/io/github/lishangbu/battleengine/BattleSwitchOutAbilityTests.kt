package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFormProfile
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证成员离场时触发的特性效果。 */
class BattleSwitchOutAbilityTests {
	@Test
	fun `natural cure and regenerator apply before holder leaves the field`() {
		val holder = participant(
			"holder",
			100,
			currentHp = 40,
			abilityEffects = listOf(
				BattleAbilityEffect.SwitchOutMajorStatusCure(),
				BattleAbilityEffect.SwitchOutHeal(3),
			),
		).copy(majorStatus = BattleMajorStatus.POISON)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = holder,
					second = participant("opponent", 50),
					firstBench = listOf(participant("reserve", 80)),
				),
			),
			listOf(BattleAction.SwitchParticipant("holder", "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(73, resolved.participant("holder")?.currentHp)
		assertEquals(null, resolved.participant("holder")?.majorStatus)
	}

	@Test
	fun `zero to hero changes form after a voluntary switch`() {
		val zero = palafinForm(964, attack = 70)
		val hero = palafinForm(10256, attack = 160)
		val holder = palafin(zero, hero)
		val engine = BattleEngine()

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = holder,
					second = participant("opponent", 50),
					firstBench = listOf(participant("reserve", 80)),
				),
			),
			listOf(BattleAction.SwitchParticipant("palafin", "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(hero.creatureId, resolved.participant("palafin")?.creatureId)
		assertEquals(hero.attack, resolved.participant("palafin")?.attack)
		assertEquals(
			listOf(hero.creatureId),
			resolved.events.filterIsInstance<BattleEvent.FormChanged>().map { it.toCreatureId },
		)
	}

	@Test
	fun `zero to hero also changes form after a forced switch`() {
		val zero = palafinForm(964, attack = 70)
		val hero = palafinForm(10256, attack = 160)
		val forceSwitchSkill = damagingSkill(skillId = 509, power = 1, forceTargetSwitch = true)
		val engine = BattleEngine()

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = palafin(zero, hero),
					second = participant("opponent", 100, skill = forceSwitchSkill),
					firstBench = listOf(participant("reserve", 80)),
				),
			),
			listOf(BattleAction.UseSkill("opponent", forceSwitchSkill.skillId, "palafin")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(hero.creatureId, resolved.participant("palafin")?.creatureId)
		assertEquals(hero.attack, resolved.participant("palafin")?.attack)
	}

	private fun palafin(zero: BattleFormProfile, hero: BattleFormProfile) = participant(
		actorId = "palafin",
		speed = zero.speed,
		creatureId = zero.creatureId,
		abilityEffects = listOf(BattleAbilityEffect.SwitchOutFormChange("palafin-zero", "palafin-hero")),
		battleFormProfiles = mapOf("palafin-zero" to zero, "palafin-hero" to hero),
	)

	private fun palafinForm(creatureId: Long, attack: Int) = BattleFormProfile(
		creatureId = creatureId,
		maxHp = 100,
		attack = attack,
		defense = 80,
		specialAttack = 80,
		specialDefense = 80,
		speed = 100,
		weight = 602,
		elementIds = setOf(11),
	)
}
