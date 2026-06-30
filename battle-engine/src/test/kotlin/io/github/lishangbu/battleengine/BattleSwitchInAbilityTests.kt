package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 出场特性规则测试。
 *
 * 该文件覆盖成员进入场地时立即触发的被动能力。测试只使用结构化 [BattleAbilityEffect]，不依赖具体资料名称；
 * 资料层负责把基础特性映射成这些效果。每个场景都记录公开来源，确保出场触发顺序和目标集合不是闭门推导。
 */
class BattleSwitchInAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `switch in attack drop ability triggers for initial single battle active opponent`() {
		val fixture = publicBattleRuleFixture(
			name = "switch-in-attack-drop-triggers-at-battle-start",
			inputSummary = "单打战斗开始时，己方当前上场成员拥有出场降攻特性，对手当前上场且可战斗。",
			expectedSummary = "战斗开始事件之后，对手攻击能力阶级降低 1 级，并记录通用能力阶级变化事件。",
		)

		val state = engine.start(
			initialState(
				first = participant("ability-user", speed = 100, abilityEffects = listOf(switchInAttackDrop())),
				second = participant("opponent", speed = 80),
			),
		)
		val statEvent = state.events.filterIsInstance<BattleEvent.StatStageChanged>().single()
		val battleStartedIndex = state.events.indexOfFirst { it is BattleEvent.BattleStarted }
		val statEventIndex = state.events.indexOf(statEvent)

		fixture.assertNamed("switch-in-attack-drop-triggers-at-battle-start")
		assertTrue(battleStartedIndex in 0 until statEventIndex)
		assertEquals("ability-user", statEvent.actorId)
		assertEquals("opponent", statEvent.targetActorId)
		assertEquals(BattleStat.ATTACK, statEvent.stat)
		assertEquals(-1, statEvent.delta)
		assertEquals(-1, state.participant("opponent")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `switch in attack drop ability targets both active opponents in double battle`() {
		val fixture = publicBattleRuleFixture(
			name = "switch-in-attack-drop-targets-both-double-battle-opponents",
			inputSummary = "双打战斗开始时，己方一个当前上场成员拥有出场降攻特性，对方两个当前上场成员均可战斗。",
			expectedSummary = "对方两个当前上场成员各降低攻击 1 级；己方同伴不受该出场特性影响。",
		)

		val state = engine.start(
			doubleInitialState(
				firstA = participant("ability-user", speed = 100, abilityEffects = listOf(switchInAttackDrop())),
				firstB = participant("ally", speed = 90),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70),
			),
		)
		val statEvents = state.events.filterIsInstance<BattleEvent.StatStageChanged>()

		fixture.assertNamed("switch-in-attack-drop-targets-both-double-battle-opponents")
		assertEquals(listOf("opponent-left", "opponent-right"), statEvents.map { it.targetActorId })
		assertEquals(-1, state.participant("opponent-left")?.statStage(BattleStat.ATTACK))
		assertEquals(-1, state.participant("opponent-right")?.statStage(BattleStat.ATTACK))
		assertEquals(0, state.participant("ally")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `switch in attack drop ability triggers after voluntary switch`() {
		val fixture = publicBattleRuleFixture(
			name = "switch-in-attack-drop-triggers-after-voluntary-switch",
			inputSummary = "单打中己方主动替换到拥有出场降攻特性的后备成员，对手当前上场且可战斗。",
			expectedSummary = "替换事件先记录，随后出场特性降低对手攻击 1 级。",
		)
		val state = engine.start(
			initialState(
				first = participant("front", speed = 100),
				firstBench = listOf(
					participant("ability-user", speed = 60, abilityEffects = listOf(switchInAttackDrop())),
				),
				second = participant("opponent", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("front", targetActorId = "ability-user")),
			ScriptedBattleRandom(emptyList()),
		)
		val switchIndex = resolved.events.indexOfFirst { it is BattleEvent.ParticipantSwitched }
		val statIndex = resolved.events.indexOfFirst {
			it is BattleEvent.StatStageChanged && it.actorId == "ability-user"
		}

		fixture.assertNamed("switch-in-attack-drop-triggers-after-voluntary-switch")
		assertTrue(switchIndex in 0 until statIndex)
		assertEquals(-1, resolved.participant("opponent")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `switch in weather ability starts weather at battle start`() {
		val fixture = publicBattleRuleFixture(
			name = "switch-in-weather-starts-rain-at-battle-start",
			inputSummary = "单打战斗开始时，己方当前上场成员拥有出场设置下雨天气的结构化特性。",
			expectedSummary = "战斗开始事件之后，天气变为下雨并写入 5 回合持续时间，同时记录天气开始事件。",
		)

		val state = engine.start(
			initialState(
				first = participant("weather-user", speed = 100, abilityEffects = listOf(switchInWeather(BattleWeather.RAIN))),
				second = participant("opponent", speed = 80),
			),
		)
		val weatherEvent = state.events.filterIsInstance<BattleEvent.WeatherStarted>().single()
		val battleStartedIndex = state.events.indexOfFirst { it is BattleEvent.BattleStarted }
		val weatherEventIndex = state.events.indexOf(weatherEvent)

		fixture.assertNamed("switch-in-weather-starts-rain-at-battle-start")
		assertTrue(battleStartedIndex in 0 until weatherEventIndex)
		assertEquals("weather-user", weatherEvent.actorId)
		assertEquals(BattleWeather.RAIN, weatherEvent.weather)
		assertEquals(5, weatherEvent.turnsRemaining)
		assertEquals(BattleWeather.RAIN, state.environment.weather)
		assertEquals(5, state.environment.weatherTurnsRemaining)
	}

	@Test
	fun `weather extending item makes switch in weather ability last eight turns`() {
		val fixture = publicBattleRuleFixture(
			name = "weather-extending-item-makes-weather-ability-last-eight-turns",
			inputSummary = "单打战斗开始时，己方当前上场成员拥有出场设置下雨天气特性，并携带匹配天气的延长道具。",
			expectedSummary = "战斗开始后下雨天气按 8 回合建立，并产生记录 8 回合的天气开始事件。",
		)

		val state = engine.start(
			initialState(
				first = participant(
					"weather-user",
					speed = 100,
					abilityEffects = listOf(switchInWeather(BattleWeather.RAIN)),
					itemId = 262,
					itemEffects = listOf(
						BattleItemEffect.WeatherDurationExtension(
							weathers = setOf(BattleWeather.RAIN),
							turnsRemaining = 8,
						),
					),
				),
				second = participant("opponent", speed = 80),
			),
		)
		val weatherEvent = state.events.filterIsInstance<BattleEvent.WeatherStarted>().single()

		fixture.assertNamed("weather-extending-item-makes-weather-ability-last-eight-turns")
		assertEquals("weather-user", weatherEvent.actorId)
		assertEquals(BattleWeather.RAIN, weatherEvent.weather)
		assertEquals(8, weatherEvent.turnsRemaining)
		assertEquals(BattleWeather.RAIN, state.environment.weather)
		assertEquals(8, state.environment.weatherTurnsRemaining)
	}

	@Test
	fun `slower initial weather ability overwrites faster weather ability`() {
		val fixture = publicBattleRuleFixture(
			name = "slower-switch-in-weather-overwrites-faster-weather-at-battle-start",
			inputSummary = "单打战斗开始时，双方当前上场成员分别拥有出场设置天气的特性；己方速度更快，对方速度更慢。",
			expectedSummary = "较快成员先设置下雨，较慢成员后设置大晴天，最终保留较慢成员设置的天气。",
		)

		val state = engine.start(
			initialState(
				first = participant("rain-user", speed = 100, abilityEffects = listOf(switchInWeather(BattleWeather.RAIN))),
				second = participant("sun-user", speed = 80, abilityEffects = listOf(switchInWeather(BattleWeather.SUN))),
			),
		)
		val weatherEvents = state.events.filterIsInstance<BattleEvent.WeatherStarted>()

		fixture.assertNamed("slower-switch-in-weather-overwrites-faster-weather-at-battle-start")
		assertEquals(listOf("rain-user", "sun-user"), weatherEvents.map { it.actorId })
		assertEquals(listOf(BattleWeather.RAIN, BattleWeather.SUN), weatherEvents.map { it.weather })
		assertEquals(BattleWeather.SUN, state.environment.weather)
		assertEquals(5, state.environment.weatherTurnsRemaining)
	}

	@Test
	fun `switch in weather ability triggers after voluntary switch`() {
		val fixture = publicBattleRuleFixture(
			name = "switch-in-weather-triggers-after-voluntary-switch",
			inputSummary = "单打中己方主动替换到拥有出场设置沙暴天气特性的后备成员。",
			expectedSummary = "替换事件先记录，随后天气变为沙暴并写入 5 回合持续时间。",
		)
		val state = engine.start(
			initialState(
				first = participant("front", speed = 100),
				firstBench = listOf(
					participant("weather-user", speed = 60, abilityEffects = listOf(switchInWeather(BattleWeather.SANDSTORM))),
				),
				second = participant("opponent", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("front", targetActorId = "weather-user")),
			ScriptedBattleRandom(emptyList()),
		)
		val switchIndex = resolved.events.indexOfFirst { it is BattleEvent.ParticipantSwitched }
		val weatherIndex = resolved.events.indexOfFirst {
			it is BattleEvent.WeatherStarted && it.actorId == "weather-user"
		}
		val weatherEvent = resolved.events.filterIsInstance<BattleEvent.WeatherStarted>().single()

		fixture.assertNamed("switch-in-weather-triggers-after-voluntary-switch")
		assertTrue(switchIndex in 0 until weatherIndex)
		assertEquals(5, weatherEvent.turnsRemaining)
		assertEquals(BattleWeather.SANDSTORM, resolved.environment.weather)
		assertEquals(4, resolved.environment.weatherTurnsRemaining)
	}

	@Test
	fun `switch in terrain ability starts terrain at battle start`() {
		val fixture = publicBattleRuleFixture(
			name = "switch-in-terrain-starts-electric-terrain-at-battle-start",
			inputSummary = "单打战斗开始时，己方当前上场成员拥有出场设置电气场地的结构化特性。",
			expectedSummary = "战斗开始事件之后，场地变为电气场地并写入 5 回合持续时间，同时记录场地开始事件。",
		)

		val state = engine.start(
			initialState(
				first = participant("terrain-user", speed = 100, abilityEffects = listOf(switchInTerrain(BattleTerrain.ELECTRIC))),
				second = participant("opponent", speed = 80),
			),
		)
		val terrainEvent = state.events.filterIsInstance<BattleEvent.TerrainStarted>().single()
		val battleStartedIndex = state.events.indexOfFirst { it is BattleEvent.BattleStarted }
		val terrainEventIndex = state.events.indexOf(terrainEvent)

		fixture.assertNamed("switch-in-terrain-starts-electric-terrain-at-battle-start")
		assertTrue(battleStartedIndex in 0 until terrainEventIndex)
		assertEquals("terrain-user", terrainEvent.actorId)
		assertEquals(BattleTerrain.ELECTRIC, terrainEvent.terrain)
		assertEquals(5, terrainEvent.turnsRemaining)
		assertEquals(BattleTerrain.ELECTRIC, state.environment.terrain)
		assertEquals(5, state.environment.terrainTurnsRemaining)
	}

	@Test
	fun `terrain extending item makes switch in terrain ability last eight turns`() {
		val fixture = publicBattleRuleFixture(
			name = "terrain-extending-item-makes-terrain-ability-last-eight-turns",
			inputSummary = "单打战斗开始时，己方当前上场成员拥有出场设置精神场地特性，并携带场地延长道具。",
			expectedSummary = "战斗开始后精神场地按 8 回合建立，并产生记录 8 回合的场地开始事件。",
		)

		val state = engine.start(
			initialState(
				first = participant(
					"terrain-user",
					speed = 100,
					abilityEffects = listOf(switchInTerrain(BattleTerrain.PSYCHIC)),
					itemId = 896,
					itemEffects = listOf(
						BattleItemEffect.TerrainDurationExtension(
							terrains = setOf(
								BattleTerrain.ELECTRIC,
								BattleTerrain.GRASSY,
								BattleTerrain.MISTY,
								BattleTerrain.PSYCHIC,
							),
							turnsRemaining = 8,
						),
					),
				),
				second = participant("opponent", speed = 80),
			),
		)
		val terrainEvent = state.events.filterIsInstance<BattleEvent.TerrainStarted>().single()

		fixture.assertNamed("terrain-extending-item-makes-terrain-ability-last-eight-turns")
		assertEquals("terrain-user", terrainEvent.actorId)
		assertEquals(BattleTerrain.PSYCHIC, terrainEvent.terrain)
		assertEquals(8, terrainEvent.turnsRemaining)
		assertEquals(BattleTerrain.PSYCHIC, state.environment.terrain)
		assertEquals(8, state.environment.terrainTurnsRemaining)
	}

	@Test
	fun `slower initial terrain ability overwrites faster terrain ability`() {
		val fixture = publicBattleRuleFixture(
			name = "slower-switch-in-terrain-overwrites-faster-terrain-at-battle-start",
			inputSummary = "单打战斗开始时，双方当前上场成员分别拥有出场设置场地的特性；己方速度更快，对方速度更慢。",
			expectedSummary = "较快成员先设置电气场地，较慢成员后设置精神场地，最终保留较慢成员设置的场地。",
		)

		val state = engine.start(
			initialState(
				first = participant("electric-user", speed = 100, abilityEffects = listOf(switchInTerrain(BattleTerrain.ELECTRIC))),
				second = participant("psychic-user", speed = 80, abilityEffects = listOf(switchInTerrain(BattleTerrain.PSYCHIC))),
			),
		)
		val terrainEvents = state.events.filterIsInstance<BattleEvent.TerrainStarted>()

		fixture.assertNamed("slower-switch-in-terrain-overwrites-faster-terrain-at-battle-start")
		assertEquals(listOf("electric-user", "psychic-user"), terrainEvents.map { it.actorId })
		assertEquals(listOf(BattleTerrain.ELECTRIC, BattleTerrain.PSYCHIC), terrainEvents.map { it.terrain })
		assertEquals(BattleTerrain.PSYCHIC, state.environment.terrain)
		assertEquals(5, state.environment.terrainTurnsRemaining)
	}

	@Test
	fun `switch in terrain ability triggers after voluntary switch`() {
		val fixture = publicBattleRuleFixture(
			name = "switch-in-terrain-triggers-after-voluntary-switch",
			inputSummary = "单打中己方主动替换到拥有出场设置薄雾场地特性的后备成员。",
			expectedSummary = "替换事件先记录，随后场地变为薄雾场地并写入 5 回合持续时间。",
		)
		val state = engine.start(
			initialState(
				first = participant("front", speed = 100),
				firstBench = listOf(
					participant("terrain-user", speed = 60, abilityEffects = listOf(switchInTerrain(BattleTerrain.MISTY))),
				),
				second = participant("opponent", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("front", targetActorId = "terrain-user")),
			ScriptedBattleRandom(emptyList()),
		)
		val switchIndex = resolved.events.indexOfFirst { it is BattleEvent.ParticipantSwitched }
		val terrainIndex = resolved.events.indexOfFirst {
			it is BattleEvent.TerrainStarted && it.actorId == "terrain-user"
		}
		val terrainEvent = resolved.events.filterIsInstance<BattleEvent.TerrainStarted>().single()

		fixture.assertNamed("switch-in-terrain-triggers-after-voluntary-switch")
		assertTrue(switchIndex in 0 until terrainIndex)
		assertEquals(5, terrainEvent.turnsRemaining)
		assertEquals(BattleTerrain.MISTY, resolved.environment.terrain)
		assertEquals(4, resolved.environment.terrainTurnsRemaining)
	}

	private fun switchInAttackDrop(): BattleAbilityEffect.SwitchInStatStageChange =
		BattleAbilityEffect.SwitchInStatStageChange(
			stat = BattleStat.ATTACK,
			stageDelta = -1,
		)

	private fun switchInWeather(weather: BattleWeather): BattleAbilityEffect.SwitchInWeatherChange =
		BattleAbilityEffect.SwitchInWeatherChange(weather = weather, turnsRemaining = 5)

	private fun switchInTerrain(terrain: BattleTerrain): BattleAbilityEffect.SwitchInTerrainChange =
		BattleAbilityEffect.SwitchInTerrainChange(terrain = terrain, turnsRemaining = 5)
}
