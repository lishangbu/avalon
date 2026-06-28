package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleStat
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
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Intimidate_(Ability)",
			),
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
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Intimidate_(Ability)",
			),
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
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
			),
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

	private fun switchInAttackDrop(): BattleAbilityEffect.SwitchInStatStageChange =
		BattleAbilityEffect.SwitchInStatStageChange(
			stat = BattleStat.ATTACK,
			stageDelta = -1,
		)
}
