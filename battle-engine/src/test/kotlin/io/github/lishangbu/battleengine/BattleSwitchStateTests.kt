package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证替换成员时的运行态清理。
 *
 * 场景类型：换人状态 fixture。
 * 参考来源类型：现代主系列替换规则的稳定行为；本测试只覆盖离场时应清理/保留的运行态边界，
 * 不覆盖入场特性、踩钉、天气入场伤害或强制换人优先级。
 * 验证重点：能力阶级和连续保护计数随离场清空，HP、PP 与主要异常状态仍保留。
 */
class BattleSwitchStateTests {
	private val engine = BattleEngine()

	@Test
	fun `switching out clears stat stages and protection chain but keeps major status`() {
		val starter = participant("starter", speed = 100).copy(
			majorStatus = BattleMajorStatus.BURN,
			statStages = mapOf(BattleStat.ATTACK to 2, BattleStat.SPEED to -1),
			protectionChain = 1,
		)
		val state = engine.start(
			initialState(
				first = starter,
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("observer", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("starter", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		val switchedOut = resolved.participant("starter")
		assertEquals(BattleMajorStatus.BURN, switchedOut?.majorStatus)
		assertEquals(0, switchedOut?.statStage(BattleStat.ATTACK))
		assertEquals(0, switchedOut?.statStage(BattleStat.SPEED))
		assertEquals(0, switchedOut?.protectionChain)
		assertEquals(35, switchedOut?.skillSlot(1)?.remainingPp)
	}
}
