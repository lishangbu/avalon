package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BattleBoosterEnergyItemTests {
	private val effect = BattleItemEffect.HighestStatBoosterActivation(setOf(281, 282))

	@Test
	fun `booster energy activates highest raw stat for matching ability`() {
		val holder = participant(
			"holder", 100, abilityId = 281, itemId = 1696, itemEffects = listOf(effect),
		).copy(attack = 130)
		val started = BattleEngine().start(initialState(first = holder, second = participant("target", 50)))
		val activated = started.events.filterIsInstance<BattleEvent.HeldItemHighestStatBoostActivated>().single()

		assertEquals(BattleStat.ATTACK, started.participant("holder")?.boosterEnergyStat)
		assertNull(started.participant("holder")?.itemId)
		assertEquals(BattleStat.ATTACK, activated.stat)
		assertEquals(1.3, activated.multiplier)
	}

	@Test
	fun `booster energy speed marker changes action order`() {
		val holder = participant(
			"holder", 80, abilityId = 282, itemId = 1696, itemEffects = listOf(effect),
		).copy(attack = 50, defense = 50, specialAttack = 50, specialDefense = 50)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = holder, second = participant("target", 100))),
			listOf(
				BattleAction.UseSkill("holder", 1, "target"),
				BattleAction.UseSkill("target", 1, "holder"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		assertEquals(BattleStat.SPEED, resolved.participant("holder")?.boosterEnergyStat)
		assertEquals("holder", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
	}

	@Test
	fun `booster energy remains held for wrong ability or active matching weather`() {
		val wrongAbility = participant(
			"wrong", 100, abilityId = 1, itemId = 1696, itemEffects = listOf(effect),
		).copy(attack = 130)
		val wrongStarted = BattleEngine().start(initialState(first = wrongAbility, second = participant("target", 50)))
		assertEquals(1696, wrongStarted.participant("wrong")?.itemId)

		val weatherHolder = participant(
			"weather-holder", 100, abilityId = 281, itemId = 1696, itemEffects = listOf(effect),
		).copy(attack = 130)
		val weatherStarted = BattleEngine().start(
			initialState(
				first = weatherHolder,
				second = participant("target", 50),
				environment = BattleEnvironment(weather = BattleWeather.SUN),
			),
		)
		assertEquals(1696, weatherStarted.participant("weather-holder")?.itemId)
		assertNull(weatherStarted.participant("weather-holder")?.boosterEnergyStat)
	}
}
