package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 验证战斗格式快照和初始状态的不变量。
 *
 * 场景类型：格式级 fixture。
 * 参考来源类型：现代主系列常见单打/双打站位规则；本测试不覆盖具体条款限制，只保证引擎入口不会接受
 * 与格式声明矛盾的队伍和上场席位。
 * 验证重点：单打/双打上场数量、整场成员 ID 唯一性和队伍规模限制都在战斗开始前失败。
 */
class BattleFormatValidationTests {
	private val engine = BattleEngine()

	@Test
	fun `double format accepts two active participants per side`() {
		val state = doubleInitialState()

		assertEquals(BattleMode.DOUBLE, state.format.mode)
		assertEquals(2, state.sides.single { it.sideId == "side-a" }.activeActorIds.size)
		assertEquals(2, state.sides.single { it.sideId == "side-b" }.activeActorIds.size)
	}

	@Test
	fun `format active participant count must match battle mode`() {
		assertFailsWith<IllegalArgumentException> {
			BattleFormatSnapshot(
				code = "bad-double",
				mode = BattleMode.DOUBLE,
				activeParticipantsPerSide = 1,
			)
		}
	}

	@Test
	fun `initial state rejects duplicated actor ids across sides`() {
		val duplicated = participant("duplicated", speed = 100)

		assertFailsWith<IllegalArgumentException> {
			BattleInitialState(
				format = singleFormat(),
				rules = neutralRules(),
				sides = listOf(
					BattleSide("side-a", listOf("duplicated"), listOf(duplicated)),
					BattleSide("side-b", listOf("duplicated"), listOf(duplicated.copy(speed = 80))),
				),
			)
		}
	}

	@Test
	fun `initial state rejects teams larger than format team size`() {
		assertFailsWith<IllegalArgumentException> {
			BattleInitialState(
				format = singleFormat(teamSize = 1),
				rules = neutralRules(),
				sides = listOf(
					BattleSide(
						"side-a",
						listOf("side-a-active"),
						listOf(
							participant("side-a-active", speed = 100),
							participant("side-a-reserve", speed = 90),
						),
					),
					BattleSide("side-b", listOf("side-b-active"), listOf(participant("side-b-active", speed = 80))),
				),
			)
		}
	}

	@Test
	fun `max turn limit ends battle as draw after end turn effects`() {
		val fixture = publicBattleRuleFixture(
			name = "max-turn-limit-ends-battle-as-draw-after-end-turn-effects",
			inputSummary = "格式声明最大回合数为 1，双方在第 1 回合结束时都仍可战斗。",
			expectedSummary = "引擎在完整回合末按回合上限产生平局结果，不再追加普通回合结束事件。",
		)
		val state = engine.start(
			initialState().copy(format = singleFormat().copy(maxTurns = 1)),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("max-turn-limit-ends-battle-as-draw-after-end-turn-effects")
		assertEquals(null, resolved.result?.winningSideId)
		assertEquals("max-turns-reached", resolved.result?.reason)
		val ended = resolved.events.filterIsInstance<BattleEvent.BattleEnded>().single()
		assertEquals(null, ended.winningSideId)
		assertEquals("max-turns-reached", ended.reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.TurnEnded>())
	}
}
