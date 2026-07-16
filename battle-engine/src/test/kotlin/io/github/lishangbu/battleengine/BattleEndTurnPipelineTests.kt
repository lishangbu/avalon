package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证完整回合末流水线的阶段顺序。
 *
 * 单独的异常、束缚、天气、场地和道具测试已经覆盖各自数值；本测试专门把多个回合末效果压到同一回合里，固定
 * 它们的相对顺序。这样后续继续瘦身 [BattleEndTurnEffects] 或 [BattleTurnResolution] 时，只要事件顺序被改乱，
 * 这里会比单点测试更早失败。
 */
class BattleEndTurnPipelineTests {
	private val engine = BattleEngine()

	@Test
	fun `black sludge heals poison holder and damages non poison holder`() {
		val effects = listOf(
			BattleItemEffect.HeldEndTurnHealForElement(elementId = 4, healDenominator = 16),
			BattleItemEffect.HeldEndTurnDamageWithoutElement(elementId = 4, damageDenominator = 8),
		)
		val state = engine.start(
			initialState(
				first = participant(
					"poison-holder",
					speed = 100,
					elementId = 4,
					currentHp = 50,
					itemId = 281,
					itemEffects = effects,
				),
				second = participant(
					"normal-holder",
					speed = 50,
					currentHp = 100,
					itemId = 281,
					itemEffects = effects,
				),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		assertEquals(56, resolved.participant("poison-holder")?.currentHp)
		assertEquals(88, resolved.participant("normal-holder")?.currentHp)
		assertEquals(listOf(6), resolved.events.filterIsInstance<BattleEvent.HealingApplied>().map { it.amount })
		assertEquals(listOf(12), resolved.events.filterIsInstance<BattleEvent.HeldItemDamageApplied>().map { it.amount })
	}

	@Test
	fun `held status orbs apply burn and bad poison after end turn damage`() {
		val state = engine.start(
			initialState(
				first = participant(
					"flame-orb-holder",
					speed = 100,
					itemId = 273,
					itemEffects = listOf(BattleItemEffect.HeldEndTurnMajorStatus(BattleMajorStatus.BURN)),
				),
				second = participant(
					"toxic-orb-holder",
					speed = 50,
					itemId = 272,
					itemEffects = listOf(BattleItemEffect.HeldEndTurnMajorStatus(BattleMajorStatus.BAD_POISON)),
				),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		assertEquals(BattleMajorStatus.BURN, resolved.participant("flame-orb-holder")?.majorStatus)
		assertEquals(BattleMajorStatus.BAD_POISON, resolved.participant("toxic-orb-holder")?.majorStatus)
		assertEquals(
			listOf("flame-orb-holder", "toxic-orb-holder"),
			resolved.events.filterIsInstance<BattleEvent.StatusApplied>().map { it.targetActorId },
		)
		assertEquals(100, resolved.participant("flame-orb-holder")?.currentHp)
		assertEquals(100, resolved.participant("toxic-orb-holder")?.currentHp)
	}

	@Test
	fun `end turn pipeline applies residual binding weather weather healing terrain healing and item healing in order`() {
		val state = engine.start(
			initialState(
				first = participant(
					"end-turn-target",
					speed = 100,
					currentHp = 70,
					abilityEffects = listOf(BattleAbilityEffect.WeatherEndTurnHeal(setOf(BattleWeather.SANDSTORM), healDenominator = 16)),
					itemId = 30,
					itemEffects = listOf(BattleItemEffect.HeldEndTurnHeal(healDenominator = 16)),
				).copy(
					majorStatus = BattleMajorStatus.BURN,
					boundByActorId = "binder",
					bindingTurnsRemaining = 2,
				),
				second = participant("binder", speed = 50, elementId = 6),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM, terrain = BattleTerrain.GRASSY),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))
		val pipelineEvents = resolved.events.filter {
			it is BattleEvent.ResidualDamageApplied ||
				it is BattleEvent.BindingDamageApplied ||
				it is BattleEvent.WeatherDamageApplied ||
				it is BattleEvent.WeatherHealingApplied ||
				it is BattleEvent.TerrainHealingApplied ||
				it is BattleEvent.HealingApplied
		}

		assertEquals(
			listOf(
				BattleEvent.ResidualDamageApplied::class,
				BattleEvent.BindingDamageApplied::class,
				BattleEvent.WeatherDamageApplied::class,
				BattleEvent.WeatherHealingApplied::class,
				BattleEvent.TerrainHealingApplied::class,
				BattleEvent.HealingApplied::class,
			),
			pipelineEvents.map { it::class },
		)
		assertEquals(listOf(6), pipelineEvents.filterIsInstance<BattleEvent.ResidualDamageApplied>().map { it.amount })
		assertEquals(listOf(12), pipelineEvents.filterIsInstance<BattleEvent.BindingDamageApplied>().map { it.amount })
		assertEquals(listOf(6), pipelineEvents.filterIsInstance<BattleEvent.WeatherDamageApplied>().map { it.amount })
		assertEquals(listOf(6), pipelineEvents.filterIsInstance<BattleEvent.WeatherHealingApplied>().map { it.amount })
		assertEquals(listOf(6), pipelineEvents.filterIsInstance<BattleEvent.TerrainHealingApplied>().map { it.amount })
		assertEquals(listOf(6), pipelineEvents.filterIsInstance<BattleEvent.HealingApplied>().map { it.amount })
		assertEquals(64, resolved.participant("end-turn-target")?.currentHp)
		assertEquals(1, resolved.participant("end-turn-target")?.bindingTurnsRemaining)
	}

	@Test
	fun `end turn pipeline stops later phases after residual damage ends battle`() {
		val state = engine.start(
			initialState(
				first = participant(
					"fainting-target",
					speed = 100,
					currentHp = 5,
					abilityEffects = listOf(BattleAbilityEffect.WeatherEndTurnHeal(setOf(BattleWeather.SANDSTORM), healDenominator = 16)),
					itemId = 30,
					itemEffects = listOf(BattleItemEffect.HeldEndTurnHeal(healDenominator = 16)),
				).copy(
					majorStatus = BattleMajorStatus.BURN,
					boundByActorId = "binder",
					bindingTurnsRemaining = 2,
				),
				second = participant("binder", speed = 50, elementId = 6),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM, terrain = BattleTerrain.GRASSY),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		assertEquals(0, resolved.participant("fainting-target")?.currentHp)
		assertEquals("side-b", resolved.result?.winningSideId)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.ResidualDamageApplied>().size)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.BindingDamageApplied>().isEmpty())
		assertTrue(resolved.events.filterIsInstance<BattleEvent.WeatherDamageApplied>().isEmpty())
		assertTrue(resolved.events.filterIsInstance<BattleEvent.WeatherHealingApplied>().isEmpty())
		assertTrue(resolved.events.filterIsInstance<BattleEvent.TerrainHealingApplied>().isEmpty())
		assertTrue(resolved.events.filterIsInstance<BattleEvent.HealingApplied>().isEmpty())
	}

	@Test
	fun `residual damage phase stops remaining participants after battle ends`() {
		val state = engine.start(
			initialState(
				first = participant("fainting-target", speed = 100, currentHp = 5).copy(
					majorStatus = BattleMajorStatus.BURN,
				),
				second = participant("later-target", speed = 50).copy(
					majorStatus = BattleMajorStatus.BURN,
				),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))
		val residualEvents = resolved.events.filterIsInstance<BattleEvent.ResidualDamageApplied>()

		assertEquals("side-b", resolved.result?.winningSideId)
		assertEquals(listOf("fainting-target"), residualEvents.map { it.actorId })
		assertEquals(100, resolved.participant("later-target")?.currentHp)
	}

	@Test
	fun `weather damage phase stops remaining participants after battle ends`() {
		val state = engine.start(
			initialState(
				first = participant("fainting-target", speed = 100, currentHp = 5),
				second = participant("later-target", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))
		val weatherEvents = resolved.events.filterIsInstance<BattleEvent.WeatherDamageApplied>()

		assertEquals("side-b", resolved.result?.winningSideId)
		assertEquals(listOf("fainting-target"), weatherEvents.map { it.actorId })
		assertEquals(100, resolved.participant("later-target")?.currentHp)
	}

	@Test
	fun `held item damage phase stops remaining participants after battle ends`() {
		val endTurnDamage = BattleItemEffect.HeldEndTurnDamage(damageDenominator = 8)
		val state = engine.start(
			initialState(
				first = participant(
					"fainting-target",
					speed = 100,
					currentHp = 5,
					itemId = 265,
					itemEffects = listOf(endTurnDamage),
				),
				second = participant(
					"later-target",
					speed = 50,
					itemId = 265,
					itemEffects = listOf(endTurnDamage),
				),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))
		val itemDamageEvents = resolved.events.filterIsInstance<BattleEvent.HeldItemDamageApplied>()

		assertEquals("side-b", resolved.result?.winningSideId)
		assertEquals(listOf("fainting-target"), itemDamageEvents.map { it.actorId })
		assertEquals(100, resolved.participant("later-target")?.currentHp)
	}
}
