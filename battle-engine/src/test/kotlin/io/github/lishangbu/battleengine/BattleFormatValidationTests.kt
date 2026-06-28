package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleSide
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
}
