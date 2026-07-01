package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleItemEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 验证 `BattleParticipant` 的技能槽、携带道具、蓄力、休整和锁招底层状态迁移。
 *
 * 回合引擎负责决定何时消耗 PP、何时触发讲究类道具、何时追加技能使用或疲劳混乱事件；这些扩展函数只保存成员
 * 快照上的运行态。本测试因此只断言字段变化和倒计时清理，避免把回合编排细节耦合进基础模型测试。
 */
class BattleParticipantSkillOperationsTests {
	@Test
	fun `skill slot replacement choice lock and item consumption update only skill state`() {
		val firstSkill = damagingSkill(skillId = 1, name = "一号技能")
		val secondSkill = damagingSkill(skillId = 2, name = "二号技能")
		val actor = participant(
			"choice-user",
			speed = 100,
			skill = firstSkill,
			itemId = 10,
			itemEffects = listOf(BattleItemEffect.ChoiceSkillLock(speedMultiplier = 1.5)),
		).copy(skillSlots = listOf(firstSkill, secondSkill))

		val afterPp = actor.replaceSkillSlot(secondSkill.consumePp())
		val locked = afterPp.lockChoiceSkillIfNeeded(secondSkill.skillId)
		val itemConsumed = locked.consumeHeldItem()

		assertEquals(35, afterPp.skillSlot(firstSkill.skillId)?.remainingPp)
		assertEquals(34, afterPp.skillSlot(secondSkill.skillId)?.remainingPp)
		assertFalse(locked.choiceLockedToAnotherSkill(secondSkill.skillId))
		assertTrue(locked.choiceLockedToAnotherSkill(firstSkill.skillId))
		assertNull(itemConsumed.itemId)
		assertEquals(emptyList(), itemConsumed.itemEffects)
		assertNull(itemConsumed.choiceLockedSkillId)
	}

	@Test
	fun `charging recharge and locked move counters clear their related fields`() {
		val actor = participant("timed-skill-user", speed = 100)

		val charging = actor.startChargingSkill(skillId = 1, targetActorId = "target", turnsRemainingBeforeUse = 2)
		val chargingAfterOneWait = charging.consumeChargingTurn()
		val chargingCleared = chargingAfterOneWait.consumeChargingTurn()
		val recharging = actor.startRecharge(turnsRemainingAfterCurrent = 2)
		val rechargeCleared = recharging.consumeRechargeTurn().consumeRechargeTurn()
		val locked = actor.startLockedMove(
			skillId = 1,
			targetActorId = "target",
			turnsRemainingAfterCurrent = 2,
			confusesOnEnd = true,
		)
		val lockedAfterOneForcedTurn = locked.consumeLockedMoveTurn()
		val lockedCleared = lockedAfterOneForcedTurn.consumeLockedMoveTurn()

		assertEquals(1, chargingAfterOneWait.chargingTurnsRemaining)
		assertEquals(1, chargingAfterOneWait.chargingSkillId)
		assertNull(chargingCleared.chargingSkillId)
		assertNull(chargingCleared.chargingTargetActorId)
		assertEquals(0, chargingCleared.chargingTurnsRemaining)
		assertEquals(0, rechargeCleared.rechargeTurnsRemaining)
		assertEquals(1, lockedAfterOneForcedTurn.lockedMoveTurnsRemaining)
		assertTrue(lockedAfterOneForcedTurn.lockedMoveConfusesOnEnd)
		assertNull(lockedCleared.lockedMoveSkillId)
		assertNull(lockedCleared.lockedMoveTargetActorId)
		assertEquals(0, lockedCleared.lockedMoveTurnsRemaining)
		assertFalse(lockedCleared.lockedMoveConfusesOnEnd)
	}
}
