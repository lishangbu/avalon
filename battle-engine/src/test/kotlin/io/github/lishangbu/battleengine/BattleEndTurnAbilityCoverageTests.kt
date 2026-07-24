package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 验证主系列回合末伤害、治愈与随机能力变化特性的公开行为。 */
class BattleEndTurnAbilityCoverageTests {
	private val engine = BattleEngine()

	@Test
	fun `weather end turn damage hurts its holder only in the configured weather`() {
		val state = engine.start(
			initialState(
				first = participant(
					"dry-skin-holder",
					speed = 100,
					abilityEffects = listOf(
						BattleAbilityEffect.WeatherEndTurnDamage(setOf(BattleWeather.SUN), 8),
					),
				),
				second = participant("opponent", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.SUN),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		assertEquals(88, resolved.participant("dry-skin-holder")?.currentHp)
		assertEquals(100, resolved.participant("opponent")?.currentHp)
	}

	@Test
	fun `bad dreams damages sleeping opponents at end turn`() {
		val state = engine.start(
			initialState(
				first = participant(
					"bad-dreams-holder",
					speed = 100,
					abilityEffects = listOf(
						BattleAbilityEffect.OpponentMajorStatusEndTurnDamage(setOf(BattleMajorStatus.SLEEP), 8),
					),
				),
				second = participant("sleeping-target", speed = 50).copy(
					majorStatus = BattleMajorStatus.SLEEP,
					sleepTurnsRemaining = 2,
				),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		assertEquals(88, resolved.participant("sleeping-target")?.currentHp)
	}

	@Test
	fun `hydration cures its holder status in rain`() {
		val state = engine.start(
			initialState(
				first = participant(
					"hydration-holder",
					speed = 100,
					abilityEffects = listOf(
						BattleAbilityEffect.EndTurnMajorStatusCure(
							chancePercent = 100,
							requiredWeathers = setOf(BattleWeather.RAIN),
						),
					),
				).copy(majorStatus = BattleMajorStatus.BURN),
				second = participant("opponent", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.RAIN),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		assertNull(resolved.participant("hydration-holder")?.majorStatus)
	}

	@Test
	fun `healer cures an active ally when its chance succeeds`() {
		val state = engine.start(
			doubleInitialState(
				firstA = participant(
					"healer",
					speed = 100,
					abilityEffects = listOf(BattleAbilityEffect.EndTurnAllyMajorStatusCure(30)),
				),
				firstB = participant("burned-ally", speed = 90).copy(majorStatus = BattleMajorStatus.BURN),
				secondA = participant("opponent-a", speed = 80),
				secondB = participant("opponent-b", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(listOf(0)))

		assertNull(resolved.participant("burned-ally")?.majorStatus)
	}

	@Test
	fun `moody raises one random stat and lowers a different stat`() {
		val state = engine.start(
			initialState(
				first = participant(
					"moody-holder",
					speed = 100,
					abilityEffects = listOf(BattleAbilityEffect.EndTurnRandomStatStageChange(2, -1)),
				),
				second = participant("opponent", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(listOf(0, 0)))

		assertEquals(2, resolved.participant("moody-holder")?.statStages?.get(BattleStat.ATTACK))
		assertEquals(-1, resolved.participant("moody-holder")?.statStages?.get(BattleStat.DEFENSE))
	}
}
