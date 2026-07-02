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

	@Test
	fun `clear volatile status resets only the selected runtime fields`() {
		val actor = participant("volatile-clear-target", speed = 100).copy(
			flinched = true,
			confusionTurnsRemaining = 3,
			healBlockTurnsRemaining = 4,
			tauntTurnsRemaining = 2,
			disabledSkillId = 1,
			disabledSkillTurnsRemaining = 5,
			tormented = true,
			boundByActorId = "binder",
			bindingTurnsRemaining = 4,
		)

		/*
		 * `clearVolatileStatus` 是治愈道具、离场清理和来源离场解除都会复用的底层入口。这里逐个状态验证，
		 * 是为了避免后续新增临时状态时误把“清单个状态”改成“清所有临时状态”，尤其是定身法和束缚这类带
		 * 绑定字段的状态，必须在清理计数的同时同步清掉技能 ID 或来源成员 ID。
		 */
		val flinchCleared = actor.clearVolatileStatus(BattleVolatileStatus.FLINCH)
		val confusionCleared = actor.clearVolatileStatus(BattleVolatileStatus.CONFUSION)
		val healBlockCleared = actor.clearVolatileStatus(BattleVolatileStatus.HEAL_BLOCK)
		val tauntCleared = actor.clearVolatileStatus(BattleVolatileStatus.TAUNT)
		val disableCleared = actor.clearVolatileStatus(BattleVolatileStatus.DISABLE)
		val tormentCleared = actor.clearVolatileStatus(BattleVolatileStatus.TORMENT)
		val bindingCleared = actor.clearVolatileStatus(BattleVolatileStatus.BINDING)

		assertFalse(flinchCleared.flinched)
		assertEquals(3, flinchCleared.confusionTurnsRemaining)
		assertEquals(0, confusionCleared.confusionTurnsRemaining)
		assertEquals(4, confusionCleared.healBlockTurnsRemaining)
		assertEquals(0, healBlockCleared.healBlockTurnsRemaining)
		assertEquals(2, healBlockCleared.tauntTurnsRemaining)
		assertEquals(0, tauntCleared.tauntTurnsRemaining)
		assertEquals(1, tauntCleared.disabledSkillId)
		assertNull(disableCleared.disabledSkillId)
		assertEquals(0, disableCleared.disabledSkillTurnsRemaining)
		assertFalse(tormentCleared.tormented)
		assertEquals("binder", tormentCleared.boundByActorId)
		assertNull(bindingCleared.boundByActorId)
		assertEquals(0, bindingCleared.bindingTurnsRemaining)
	}
}
