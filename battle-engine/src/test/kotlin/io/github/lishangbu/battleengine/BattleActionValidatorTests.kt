package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 验证回合行动提交前的结构化校验。
 *
 * 场景类型：行动合法性 fixture。
 * 参考来源类型：现代回合制对战通用提交约束；本测试不替代事件级规则测试，只覆盖提交阶段就应被拦截的问题。
 * 验证重点：重复行动、PP 耗尽、讲究类锁定、目标不存在、替换目标非法和战斗结束后继续提交都能返回稳定 code。
 */
class BattleActionValidatorTests {
	private val engine = BattleEngine()
	private val validator = BattleActionValidator()

	@Test
	fun `valid skill action has no violations`() {
		val state = engine.start(initialState())

		val violations = validator.validate(
			state,
			listOf(BattleAction.UseSkill("side-a-active", skillId = 1, targetActorId = "side-b-active")),
		)

		assertEquals(emptyList(), violations)
	}

	@Test
	fun `reports duplicate skill pp choice lock and target violations`() {
		val emptySkill = damagingSkill(skillId = 1).copy(remainingPp = 0, maxPp = 35)
		val state = engine.start(
			initialState(
				first = participant("locked", speed = 100, skill = emptySkill).copy(choiceLockedSkillId = 2),
				second = participant("target", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(
				BattleAction.UseSkill("locked", skillId = 1, targetActorId = "missing"),
				BattleAction.UseSkill("locked", skillId = 3, targetActorId = "target"),
			),
		)

		assertEquals(
			listOf(
				"duplicate-action",
				"skill-no-pp",
				"choice-locked",
				"target-not-found",
				"duplicate-action",
				"skill-not-found",
			),
			violations.map { it.code },
		)
	}

	@Test
	fun `reports invalid switch targets`() {
		val lockedSkill = damagingSkill(lockMoveTurnsMin = 2, lockMoveTurnsMax = 2)
		val lockedActor = participant("locked", speed = 100, skill = lockedSkill).copy(
			lockedMoveSkillId = 1,
			lockedMoveTargetActorId = "opponent",
			lockedMoveTurnsRemaining = 1,
		)
		val faintedReserve = participant("fainted-reserve", speed = 80, currentHp = 0)
		val state = engine.start(
			initialState(
				first = lockedActor,
				firstBench = listOf(faintedReserve),
				second = participant("opponent", speed = 50),
			),
		)

		assertEquals(
			listOf("locked-move-prevents-switch", "switch-target-fainted"),
			validator.validate(
				state,
				listOf(BattleAction.SwitchParticipant("locked", targetActorId = "fainted-reserve")),
			).map { it.code },
		)
		assertEquals(
			listOf("locked-move-prevents-switch", "switch-target-opponent"),
			validator.validate(
				state,
				listOf(BattleAction.SwitchParticipant("locked", targetActorId = "opponent")),
			).map { it.code },
		)
		assertEquals(
			listOf("locked-move-prevents-switch", "switch-target-not-found"),
			validator.validate(
				state,
				listOf(BattleAction.SwitchParticipant("locked", targetActorId = "missing")),
			).map { it.code },
		)
	}

	@Test
	fun `allows forced switch for fainted active participant`() {
		val state = engine.start(
			initialState(
				first = participant("fainted-active", speed = 100, currentHp = 0),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("opponent", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(BattleAction.SwitchParticipant("fainted-active", targetActorId = "reserve")),
		)

		assertEquals(emptyList(), violations)
	}

	@Test
	fun `reports recharge prevents voluntary switch`() {
		val state = engine.start(
			initialState(
				first = participant("recharging", speed = 100).copy(rechargeTurnsRemaining = 1),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("opponent", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(BattleAction.SwitchParticipant("recharging", targetActorId = "reserve")),
		)

		assertEquals(listOf("recharge-prevents-switch"), violations.map { it.code })
	}

	@Test
	fun `reports charging prevents voluntary switch`() {
		val state = engine.start(
			initialState(
				first = participant("charging", speed = 100).copy(
					chargingSkillId = 1,
					chargingTargetActorId = "opponent",
					chargingTurnsRemaining = 1,
				),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("opponent", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(BattleAction.SwitchParticipant("charging", targetActorId = "reserve")),
		)

		assertEquals(listOf("charging-prevents-switch"), violations.map { it.code })
	}

	@Test
	fun `reports battle ended and require valid throws`() {
		val state = engine.start(
			initialState(
				first = participant("fast", speed = 100),
				second = participant("slow", speed = 50, currentHp = 20),
			),
		)
		val ended = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("fast", skillId = 1, targetActorId = "slow")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val actions = listOf(BattleAction.UseSkill("fast", skillId = 1, targetActorId = "slow"))

		assertEquals("battle-ended", validator.validate(ended, actions).first().code)
		assertFailsWith<IllegalArgumentException> {
			validator.requireValid(ended, actions)
		}
	}
}
