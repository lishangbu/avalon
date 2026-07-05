package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * 验证 `BattleParticipant` 的能力阶级、连续保护、剧毒计数和离场清理底层迁移。
 *
 * 能力阶级和大量临时状态只在成员处于上场席位时有意义；离场时需要清掉这些运行态，但 HP、PP、主要异常状态
 * 和携带道具本身不应被离场清理误删。本测试固定这个边界，避免后续优化换人流程时把短期状态和持久状态混在一起。
 */
class BattleParticipantStatOperationsTests {
	@Test
	fun `stat stages clamp to modern bounds and zero stage is not stored`() {
		val actor = participant("stat-user", speed = 100)

		val raised = actor.changeStatStage(BattleStat.ATTACK, 10)
		val lowered = raised.changeStatStage(BattleStat.ATTACK, -20)
		val cleared = lowered.setStatStage(BattleStat.ATTACK, 0)

		assertEquals(6, raised.statStage(BattleStat.ATTACK))
		assertEquals(-6, lowered.statStage(BattleStat.ATTACK))
		assertEquals(0, cleared.statStage(BattleStat.ATTACK))
		assertFalse(BattleStat.ATTACK in cleared.statStages)
	}

	@Test
	fun `leave battlefield clears volatile battle state but keeps persistent state`() {
		val actor = participant(
			"switching-out",
			speed = 100,
			currentHp = 80,
			itemId = 20,
			itemEffects = listOf(BattleItemEffect.ChoiceSkillLock(speedMultiplier = 1.5)),
		).copy(
			majorStatus = BattleMajorStatus.BAD_POISON,
			statStages = mapOf(BattleStat.ATTACK to 2),
			protectionChain = 2,
			fatalDamageEndureSkillId = 203,
			badPoisonCounter = 5,
			rechargeTurnsRemaining = 1,
			flinched = true,
			confusionTurnsRemaining = 3,
			healBlockTurnsRemaining = 4,
			tauntTurnsRemaining = 2,
			disabledSkillId = 1,
			disabledSkillTurnsRemaining = 2,
			tormented = true,
			boundByActorId = "binder",
			bindingTurnsRemaining = 2,
			lastSuccessfulSkillId = 1,
			lockedMoveSkillId = 1,
			lockedMoveTargetActorId = "target",
			lockedMoveTurnsRemaining = 2,
			lockedMoveConfusesOnEnd = true,
			choiceLockedSkillId = 1,
			substituteHp = 25,
		)

		val left = actor.leaveBattlefield()

		assertEquals(80, left.currentHp)
		assertEquals(BattleMajorStatus.BAD_POISON, left.majorStatus)
		assertEquals(1, left.badPoisonCounter)
		assertEquals(20, left.itemId)
		assertEquals(0, left.statStage(BattleStat.ATTACK))
		assertEquals(0, left.protectionChain)
		assertNull(left.fatalDamageEndureSkillId)
		assertEquals(0, left.rechargeTurnsRemaining)
		assertFalse(left.flinched)
		assertEquals(0, left.confusionTurnsRemaining)
		assertEquals(0, left.healBlockTurnsRemaining)
		assertEquals(0, left.tauntTurnsRemaining)
		assertNull(left.disabledSkillId)
		assertEquals(0, left.disabledSkillTurnsRemaining)
		assertFalse(left.tormented)
		assertNull(left.boundByActorId)
		assertEquals(0, left.bindingTurnsRemaining)
		assertNull(left.lastSuccessfulSkillId)
		assertNull(left.lockedMoveSkillId)
		assertNull(left.lockedMoveTargetActorId)
		assertEquals(0, left.lockedMoveTurnsRemaining)
		assertFalse(left.lockedMoveConfusesOnEnd)
		assertNull(left.choiceLockedSkillId)
		assertEquals(0, left.substituteHp)
	}
}
