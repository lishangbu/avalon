package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * 验证单打第一版状态机。
 *
 * 场景类型：状态机级 fixture。
 * 参考来源类型：公开主系列回合流程的通用阶段拆解；本测试固定随机序列，只验证行动排序、技能使用、
 * 伤害事件、倒下事件和终局事件，不覆盖尚未实现的状态、天气、特性、道具或替换。
 * 验证重点：同一初始状态、行动序列和随机脚本得到稳定事件流。
 */
class BattleEngineSingleTurnTests {
	private val engine = BattleEngine()

	@Test
	fun `start emits battle started event`() {
		val state = engine.start(initialState())

		assertEquals(0, state.turnNumber)
		assertNull(state.result)
		assertIs<BattleEvent.BattleStarted>(state.events.single())
	}

	@Test
	fun `faster participant deals damage and ends battle when target faints`() {
		val state = engine.start(
			initialState(
				first = participant("fast", speed = 100),
				second = participant("slow", speed = 50, currentHp = 20),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("fast", skillId = 1, targetActorId = "slow"),
				BattleAction.UseSkill("slow", skillId = 1, targetActorId = "fast"),
			),
			ScriptedBattleRandom(listOf(15)),
		)

		assertEquals("side-a", resolved.result?.winningSideId)
		assertEquals(0, resolved.participant("slow")?.currentHp)
		assertIs<BattleEvent.BattleEnded>(resolved.events.last())
		assertEquals(
			listOf(
				BattleEvent.BattleStarted::class,
				BattleEvent.TurnStarted::class,
				BattleEvent.SkillUsed::class,
				BattleEvent.DamageApplied::class,
				BattleEvent.ParticipantFainted::class,
				BattleEvent.BattleEnded::class,
			),
			resolved.events.map { it::class },
		)
	}

	@Test
	fun `priority acts before speed`() {
		val slowPrioritySkill = damagingSkill(skillId = 2, name = "先制测试", priority = 1)
		val state = engine.start(
			initialState(
				first = participant("slow-priority", speed = 50, skill = slowPrioritySkill),
				second = participant("fast-normal", speed = 100),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("fast-normal", skillId = 1, targetActorId = "slow-priority"),
				BattleAction.UseSkill("slow-priority", skillId = 2, targetActorId = "fast-normal"),
			),
			ScriptedBattleRandom(listOf(15, 15)),
		)

		val usedEvents = resolved.events.filterIsInstance<BattleEvent.SkillUsed>()
		assertEquals(listOf("slow-priority", "fast-normal"), usedEvents.map { it.actorId })
		assertEquals(72, resolved.participant("fast-normal")?.currentHp)
		assertEquals(72, resolved.participant("slow-priority")?.currentHp)
	}

	@Test
	fun `miss consumes accuracy random and does not apply damage`() {
		val inaccurateSkill = damagingSkill(accuracy = 50)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = inaccurateSkill),
				second = participant("defender", speed = 50),
			),
		)
		val random = ScriptedBattleRandom(listOf(99))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)

		assertEquals(100, resolved.participant("defender")?.currentHp)
		assertIs<BattleEvent.SkillMissed>(resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single())
		assertEquals(listOf("accuracy for 1"), random.consumedReasons())
	}

	@Test
	fun `status skill applies burn and end turn residual damage`() {
		val burnSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.BURN,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = burnSkill),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(BattleMajorStatus.BURN, resolved.participant("defender")?.majorStatus)
		assertEquals(94, resolved.participant("defender")?.currentHp)
		assertIs<BattleEvent.StatusApplied>(resolved.events.filterIsInstance<BattleEvent.StatusApplied>().single())
		assertIs<BattleEvent.ResidualDamageApplied>(resolved.events.filterIsInstance<BattleEvent.ResidualDamageApplied>().single())
	}

	@Test
	fun `stat stage effect changes later action damage in the same turn`() {
		val attackDropSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.ATTACK,
					target = BattleEffectTarget.TARGET,
					stageDelta = -1,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("stage-user", speed = 100, skill = attackDropSkill),
				second = participant("damager", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("stage-user", skillId = 1, targetActorId = "damager"),
				BattleAction.UseSkill("damager", skillId = 1, targetActorId = "stage-user"),
			),
			ScriptedBattleRandom(listOf(15)),
		)

		assertEquals(-1, resolved.participant("damager")?.statStage(BattleStat.ATTACK))
		assertEquals(81, resolved.participant("stage-user")?.currentHp)
		assertIs<BattleEvent.StatStageChanged>(resolved.events.filterIsInstance<BattleEvent.StatStageChanged>().single())
	}

	@Test
	fun `paralysis halves speed for action ordering`() {
		val state = engine.start(
			initialState(
				first = participant("paralyzed-fast", speed = 100).copy(majorStatus = BattleMajorStatus.PARALYSIS),
				second = participant("normal-mid", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("paralyzed-fast", skillId = 1, targetActorId = "normal-mid"),
				BattleAction.UseSkill("normal-mid", skillId = 1, targetActorId = "paralyzed-fast"),
			),
			ScriptedBattleRandom(listOf(15, 15)),
		)

		val usedEvents = resolved.events.filterIsInstance<BattleEvent.SkillUsed>()
		assertEquals(listOf("normal-mid", "paralyzed-fast"), usedEvents.map { it.actorId })
	}
}
