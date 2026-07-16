package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 验证回合行动提交前的结构化校验。
 *
 * 场景类型：行动合法性 场景。
 * 参考来源类型：现代回合制对战通用提交约束；本测试不替代事件级规则测试，只覆盖提交阶段就应被拦截的问题。
 * 验证重点：重复行动、PP 耗尽、讲究类锁定、回复封锁、挑衅、定身法、无理取闹、束缚、目标不存在、替换目标非法和
 * 战斗结束后继续提交都能返回稳定 code。
 */
class BattleActionValidatorTests {
	private val engine = BattleEngine()
	private val validator = BattleActionValidator()

	@Test
	fun `valid skill action has no violations`() {
		val scenario = publicBattleRuleScenario(
			name = "valid-skill-action-submission-passes-before-turn-resolution",
			inputSummary = "当前上场成员选择自己拥有且仍有 PP 的技能，并指向当前可战斗的对手。",
			expectedSummary = "行动提交阶段返回空违规列表，合法技能行动可以进入正式回合结算。",
		)
		val state = engine.start(initialState())

		val violations = validator.validate(
			state,
			listOf(BattleAction.UseSkill("side-a-active", skillId = 1, targetActorId = "side-b-active")),
		)

		scenario.assertNamed("valid-skill-action-submission-passes-before-turn-resolution")
		assertEquals(emptyList(), violations)
	}

	@Test
	fun `non selected target scopes ignore submitted target during validation`() {
		val selfTargetSkill = damagingSkill(skillId = 2, targetScope = BattleSkillTargetScope.SELF)
		val spreadTargetSkill = damagingSkill(skillId = 3, targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		val state = engine.start(
			initialState(
				first = participant(
					"target-scope-user",
					speed = 100,
					skill = selfTargetSkill,
				).copy(skillSlots = listOf(selfTargetSkill, spreadTargetSkill)),
				second = participant("opponent", speed = 50),
			),
		)

		val selfTargetViolations = validator.validate(
			state,
			listOf(BattleAction.UseSkill("target-scope-user", skillId = 2, targetActorId = "missing-placeholder")),
		)
		val spreadTargetViolations = validator.validate(
			state,
			listOf(BattleAction.UseSkill("target-scope-user", skillId = 3, targetActorId = "missing-placeholder")),
		)

		assertEquals(emptyList(), selfTargetViolations)
		assertEquals(emptyList(), spreadTargetViolations)
	}

	@Test
	fun `reports heal block prevents healing skill selection`() {
		val healingSkill = damagingSkill(
			skillId = 105,
			name = "自我再生",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			hpEffects = listOf(BattleSkillHpEffect.SelfHealMaxHpFraction(numerator = 1, denominator = 2)),
		)
		val attackSkill = damagingSkill(skillId = 1, name = "可选攻击")
		val state = engine.start(
			initialState(
				first = participant("healer", speed = 100, currentHp = 30, skill = healingSkill)
					.copy(skillSlots = listOf(healingSkill, attackSkill), healBlockTurnsRemaining = 2),
				second = participant("target", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(BattleAction.UseSkill("healer", skillId = 105, targetActorId = "healer")),
		)

		assertEquals(listOf("heal-blocked"), violations.map { it.code })
		assertEquals(105, violations.single().resourceId)
	}

	@Test
	fun `reports taunt prevents status skill selection`() {
		val statusSkill = damagingSkill(
			skillId = 269,
			name = "挑衅",
			damageClass = BattleDamageClass.STATUS,
			power = null,
		)
		val attackSkill = damagingSkill(skillId = 1, name = "可选攻击")
		val state = engine.start(
			initialState(
				first = participant("taunted", speed = 100, skill = statusSkill)
					.copy(skillSlots = listOf(statusSkill, attackSkill), tauntTurnsRemaining = 2),
				second = participant("target", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(BattleAction.UseSkill("taunted", skillId = 269, targetActorId = "target")),
		)

		assertEquals(listOf("taunted"), violations.map { it.code })
		assertEquals(269, violations.single().resourceId)
	}

	@Test
	fun `reports disable prevents disabled skill selection`() {
		val disabledSkill = damagingSkill(skillId = 50, name = "定身法")
		val otherSkill = damagingSkill(skillId = 1, name = "撞击")
		val state = engine.start(
			initialState(
				first = participant("disabled", speed = 100, skill = disabledSkill).copy(
					skillSlots = listOf(disabledSkill, otherSkill),
					disabledSkillId = 50,
					disabledSkillTurnsRemaining = 2,
				),
				second = participant("target", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(BattleAction.UseSkill("disabled", skillId = 50, targetActorId = "target")),
		)

		assertEquals(listOf("disabled-skill"), violations.map { it.code })
		assertEquals(50, violations.single().resourceId)
	}

	@Test
	fun `reports torment prevents repeated skill selection`() {
		val repeatedSkill = damagingSkill(skillId = 259, name = "无理取闹")
		val attackSkill = damagingSkill(skillId = 1, name = "可选攻击")
		val state = engine.start(
			initialState(
				first = participant("tormented", speed = 100, skill = repeatedSkill)
					.copy(skillSlots = listOf(repeatedSkill, attackSkill), tormented = true, lastSuccessfulSkillId = 259),
				second = participant("target", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(BattleAction.UseSkill("tormented", skillId = 259, targetActorId = "target")),
		)

		assertEquals(listOf("tormented-repeat"), violations.map { it.code })
		assertEquals(259, violations.single().resourceId)
	}

	@Test
	fun `reports duplicate skill pp choice lock and target violations`() {
		val emptySkill = damagingSkill(skillId = 1).copy(remainingPp = 0, maxPp = 35)
		val selectableChoiceSkill = damagingSkill(skillId = 2, name = "可选锁定技能")
		val state = engine.start(
			initialState(
				first = participant("locked", speed = 100, skill = emptySkill).copy(
					skillSlots = listOf(emptySkill, selectableChoiceSkill),
					choiceLockedSkillId = 2,
				),
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
	fun `allows skill submission when all carried skills must become struggle`() {
		val scenario = publicBattleRuleScenario(
			name = "action-validator-all-skills-unselectable-allows-struggle-fallback",
			inputSummary = "上场成员所有技能 PP 都已经耗尽，接口仍提交一个技能行动并带有占位目标。",
			expectedSummary = "提交阶段不返回技能 PP 或目标错误；正式回合会把该行动改为内置挣扎。",
		)
		val exhaustedSkill = damagingSkill(skillId = 1).copy(remainingPp = 0, maxPp = 35)
		val state = engine.start(
			initialState(
				first = participant("empty-pp", speed = 100, skill = exhaustedSkill),
				second = participant("target", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(BattleAction.UseSkill("empty-pp", skillId = 1, targetActorId = "missing-placeholder")),
		)

		scenario.assertNamed("action-validator-all-skills-unselectable-allows-struggle-fallback")
		assertEquals(emptyList(), violations)
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
	fun `reports duplicate switch target for healthy active participants`() {
		val violations = duplicateSwitchTargetViolations(currentHp = 100)

		assertEquals(listOf("duplicate-switch-target", "duplicate-switch-target"), violations.map { it.code })
		assertEquals(listOf("side-a-first", "side-a-second"), violations.map { it.actorId })
		assertEquals(listOf("side-a-reserve", "side-a-reserve"), violations.map { it.targetActorId })
	}

	@Test
	fun `reports duplicate switch target for fainted active participants`() {
		val violations = duplicateSwitchTargetViolations(currentHp = 0)

		assertEquals(listOf("duplicate-switch-target", "duplicate-switch-target"), violations.map { it.code })
		assertEquals(listOf("side-a-first", "side-a-second"), violations.map { it.actorId })
		assertEquals(listOf("side-a-reserve", "side-a-reserve"), violations.map { it.targetActorId })
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
	fun `switch restriction immunity item makes bound switch valid`() {
		val state = engine.start(
			initialState(
				first = participant(
					"bound",
					speed = 100,
					itemId = 295,
					itemEffects = listOf(BattleItemEffect.SwitchRestrictionImmunity()),
				).copy(boundByActorId = "binder", bindingTurnsRemaining = 2),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("binder", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(BattleAction.SwitchParticipant("bound", targetActorId = "reserve")),
		)

		assertEquals(emptyList(), violations)
	}

	@Test
	fun `reports binding prevents voluntary switch`() {
		val state = engine.start(
			initialState(
				first = participant("bound", speed = 100).copy(boundByActorId = "binder", bindingTurnsRemaining = 2),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("binder", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(BattleAction.SwitchParticipant("bound", targetActorId = "reserve")),
		)

		assertEquals(listOf("binding-prevents-switch"), violations.map { it.code })
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

	private fun duplicateSwitchTargetViolations(currentHp: Int): List<BattleActionViolation> {
		val first = participant("side-a-first", speed = 100, currentHp = currentHp)
		val second = participant("side-a-second", speed = 90, currentHp = currentHp)
		val reserve = participant("side-a-reserve", speed = 80)
		val opponent = participant("side-b-first", speed = 70)
		val opponentPartner = participant("side-b-second", speed = 60)
		val single = initialState(
			first = first,
			firstBench = listOf(second, reserve),
			second = opponent,
			secondBench = listOf(opponentPartner),
		)
		val state = engine.start(
			single.copy(
				format = single.format.copy(mode = BattleMode.DOUBLE, activeParticipantsPerSide = 2),
				sides = listOf(
					single.sides[0].copy(activeActorIds = listOf(first.actorId, second.actorId)),
					single.sides[1].copy(activeActorIds = listOf(opponent.actorId, opponentPartner.actorId)),
				),
			),
		)

		return validator.validate(
			state,
			listOf(
				BattleAction.SwitchParticipant(first.actorId, reserve.actorId),
				BattleAction.SwitchParticipant(second.actorId, reserve.actorId),
			),
		)
	}
}
