package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 验证 `BattleParticipant` 的主要异常状态和临时状态底层迁移。
 *
 * 状态是否能写入由 `BattleMajorStatusEffects` 和 `BattleVolatileStatusEffects` 负责判断；这里假设调用方已经完成
 * 免疫、替身、特性、道具和随机持续时间判定，只验证成员快照如何保存已生效状态、如何递减持续回合，以及归零
 * 时是否同步清理状态绑定字段。
 */
class BattleParticipantStatusOperationsTests {
	@Test
	fun `major status applies private counters and sleep consumption clears at zero`() {
		val actor = participant("status-target", speed = 100)

		val asleep = actor.applyMajorStatus(BattleMajorStatus.SLEEP, sleepTurnsRemaining = 2)
		val stillAsleep = asleep.consumeSleepBlockedTurn()
		val awake = stillAsleep.consumeSleepBlockedTurn()
		val poisoned = actor.applyMajorStatus(BattleMajorStatus.BAD_POISON)
		val unchanged = poisoned.applyMajorStatus(BattleMajorStatus.BURN)

		assertEquals(BattleMajorStatus.SLEEP, asleep.majorStatus)
		assertEquals(2, asleep.sleepTurnsRemaining)
		assertEquals(1, stillAsleep.sleepTurnsRemaining)
		assertNull(awake.majorStatus)
		assertEquals(0, awake.sleepTurnsRemaining)
		assertEquals(1, poisoned.badPoisonCounter)
		assertEquals(BattleMajorStatus.BAD_POISON, unchanged.majorStatus)
	}

	@Test
	fun `volatile status counters do not refresh and clear their bound fields`() {
		val actor = participant("volatile-target", speed = 100)

		val confused = actor.applyVolatileStatus(BattleVolatileStatus.CONFUSION, confusionTurnsRemaining = 3)
		val notRefreshed = confused.applyVolatileStatus(BattleVolatileStatus.CONFUSION, confusionTurnsRemaining = 5)
		val disabled = actor.applyVolatileStatus(
			status = BattleVolatileStatus.DISABLE,
			disabledSkillId = 1,
			disabledSkillTurnsRemaining = 1,
		)
		val disableCleared = disabled.decrementDisableEndTurn()
		val bound = actor.applyVolatileStatus(
			status = BattleVolatileStatus.BINDING,
			boundByActorId = "binder",
			bindingTurnsRemaining = 1,
		)
		val bindingCleared = bound.decrementBindingEndTurn()
		val tormented = actor.applyVolatileStatus(BattleVolatileStatus.TORMENT)

		assertEquals(3, confused.confusionTurnsRemaining)
		assertEquals(3, notRefreshed.confusionTurnsRemaining)
		assertNull(disableCleared.disabledSkillId)
		assertEquals(0, disableCleared.disabledSkillTurnsRemaining)
		assertNull(bindingCleared.boundByActorId)
		assertEquals(0, bindingCleared.bindingTurnsRemaining)
		assertTrue(tormented.tormented)
		assertFalse(tormented.clearVolatileStatus(BattleVolatileStatus.TORMENT).tormented)
	}
}
