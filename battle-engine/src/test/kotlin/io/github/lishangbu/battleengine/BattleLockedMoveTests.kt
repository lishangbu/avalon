package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.SwitchPreventionReason
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 验证锁招类技能的基础结算。
 *
 * 场景类型：技能执行流程 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代锁招类技能首次成功使用后会自动持续若干回合，
 * 期间不能自由换招或主动换下；持续结束后，具备疲劳标记的技能会让使用者进入混乱。
 * 验证重点：后续强制行动不重复扣 PP，玩家提交的其它技能会被锁定技能覆盖，目标按原目标席位重定向，
 * 主动替换请求被拒绝且事件流可复盘。
 */
class BattleLockedMoveTests {
	private val engine = BattleEngine()

	@Test
	fun `locked move overrides submitted skill follows target slot and confuses after final turn`() {
		val fixture = publicBattleRuleFixture(
			name = "locked-move-overrides-submitted-skill-follows-target-slot-and-confuses",
			inputSummary = "锁招技能固定持续 2 回合；第 2 回合玩家提交其它技能，同时原目标主动换到同侧后备。",
			expectedSummary = "使用者仍强制使用锁定技能命中新上场的同一目标席位，后续回合不额外扣 PP，结束后进入混乱。",
		)
		val lockingSkill = lockingSkill()
		val otherSkill = damagingSkill(skillId = 2, name = "其它动作").copy(remainingPp = 20, maxPp = 20)
		val attacker = participant("lock-user", speed = 100, skill = lockingSkill)
			.copy(skillSlots = listOf(lockingSkill, otherSkill))
		val state = engine.start(
			initialState(
				first = attacker,
				second = participant("target", speed = 50),
				secondBench = listOf(participant("target-reserve", speed = 40)),
			),
		)

		val afterFirst = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("lock-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val randomSecond = ScriptedBattleRandom(listOf(1, 15, 0))
		val afterSecond = engine.resolveTurn(
			afterFirst,
			listOf(
				BattleAction.SwitchParticipant("target", targetActorId = "target-reserve"),
				BattleAction.UseSkill("lock-user", skillId = 2, targetActorId = "target"),
			),
			randomSecond,
		)

		fixture.assertNamed("locked-move-overrides-submitted-skill-follows-target-slot-and-confuses")
		assertEquals(34, afterSecond.participant("lock-user")?.skillSlot(1)?.remainingPp)
		assertEquals(20, afterSecond.participant("lock-user")?.skillSlot(2)?.remainingPp)
		assertEquals(0, afterSecond.participant("lock-user")?.lockedMoveTurnsRemaining)
		assertEquals(2, afterSecond.participant("lock-user")?.confusionTurnsRemaining)
		assertEquals(72, afterSecond.participant("target")?.currentHp)
		assertEquals(72, afterSecond.participant("target-reserve")?.currentHp)
		assertEquals(
			listOf(1L, 1L),
			afterSecond.events
				.filterIsInstance<BattleEvent.SkillUsed>()
				.filter { it.actorId == "lock-user" }
				.map { it.skillId },
		)
		assertEquals(
			listOf("target", "target-reserve"),
			afterSecond.events
				.filterIsInstance<BattleEvent.SkillUsed>()
				.filter { it.actorId == "lock-user" }
				.map { it.targetActorId },
		)
		assertEquals(1, afterSecond.events.filterIsInstance<BattleEvent.LockedMoveStarted>().size)
		assertEquals(
			true,
			afterSecond.events.filterIsInstance<BattleEvent.LockedMoveEnded>().single().confusesUser,
		)
		assertEquals(
			BattleVolatileStatus.CONFUSION,
			afterSecond.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().single().status,
		)
		assertEquals(
			listOf(
				"critical hit for 1",
				"damage random for 1",
				"locked move confusion duration for 1",
			),
			randomSecond.consumedReasons(),
		)
	}

	@Test
	fun `locked move prevents voluntary switch and still executes forced continuation`() {
		val fixture = publicBattleRuleFixture(
			name = "locked-move-prevents-voluntary-switch-and-executes-continuation",
			inputSummary = "锁招技能固定持续 2 回合；第 2 回合使用者提交主动替换。",
			expectedSummary = "主动替换被拒绝并记录事件，使用者仍留在场上执行锁定技能，结束后疲劳混乱。",
		)
		val lockingSkill = lockingSkill()
		val state = engine.start(
			initialState(
				first = participant("lock-user", speed = 100, skill = lockingSkill),
				firstBench = listOf(participant("lock-reserve", speed = 80)),
				second = participant("target", speed = 50),
			),
		)

		val afterFirst = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("lock-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val afterSecond = engine.resolveTurn(
			afterFirst,
			listOf(BattleAction.SwitchParticipant("lock-user", targetActorId = "lock-reserve")),
			ScriptedBattleRandom(listOf(1, 15, 0)),
		)

		fixture.assertNamed("locked-move-prevents-voluntary-switch-and-executes-continuation")
		assertTrue(afterSecond.isActive("lock-user"))
		assertFalse(afterSecond.isActive("lock-reserve"))
		assertEquals(44, afterSecond.participant("target")?.currentHp)
		assertEquals(34, afterSecond.participant("lock-user")?.skillSlot(1)?.remainingPp)
		assertEquals(2, afterSecond.participant("lock-user")?.confusionTurnsRemaining)
		assertEquals(
			1,
			afterSecond.events
				.filterIsInstance<BattleEvent.SwitchPrevented>().filter { it.reason == SwitchPreventionReason.LOCKED_MOVE }
				.count { it.actorId == "lock-user" && it.skillId == 1L },
		)
		assertEquals(
			listOf(1L, 1L),
			afterSecond.events
				.filterIsInstance<BattleEvent.SkillUsed>()
				.filter { it.actorId == "lock-user" }
				.map { it.skillId },
		)
	}

	@Test
	fun `locked move disruption before final turn clears lock without fatigue confusion`() {
		val fixture = publicBattleRuleFixture(
			name = "locked-move-disruption-before-final-turn-clears-lock-without-fatigue-confusion",
			inputSummary = "锁招技能固定持续 3 回合；第 2 回合强制续用时未命中。",
			expectedSummary = "未命中会立即中断锁招；由于不是最终疲劳回合，使用者不会获得疲劳混乱。",
		)
		val lockingSkill = damagingSkill(
			name = "连续攻击测试",
			accuracy = 50,
			lockMoveTurnsMin = 3,
			lockMoveTurnsMax = 3,
			confusesUserAfterLock = true,
		)
		val state = engine.start(
			initialState(
				first = participant("lock-user", speed = 100, skill = lockingSkill),
				second = participant("target", speed = 50),
			),
		)

		val afterFirst = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("lock-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(0, 1, 15)),
		)
		val randomSecond = ScriptedBattleRandom(listOf(99))
		val afterSecond = engine.resolveTurn(afterFirst, emptyList(), randomSecond)

		fixture.assertNamed("locked-move-disruption-before-final-turn-clears-lock-without-fatigue-confusion")
		assertEquals(0, afterSecond.participant("lock-user")?.lockedMoveTurnsRemaining)
		assertEquals(0, afterSecond.participant("lock-user")?.confusionTurnsRemaining)
		assertEquals(72, afterSecond.participant("target")?.currentHp)
		assertEquals(
			false,
			afterSecond.events.filterIsInstance<BattleEvent.LockedMoveEnded>().single().confusesUser,
		)
		assertTrue(afterSecond.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().isEmpty())
		assertEquals(listOf("accuracy for 1"), randomSecond.consumedReasons())
	}

	private fun lockingSkill() =
		damagingSkill(
			name = "连续攻击测试",
			lockMoveTurnsMin = 2,
			lockMoveTurnsMax = 2,
			confusesUserAfterLock = true,
		)
}
