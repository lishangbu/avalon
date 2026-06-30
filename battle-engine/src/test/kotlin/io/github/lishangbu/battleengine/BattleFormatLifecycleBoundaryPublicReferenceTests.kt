package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 验证格式、准备校验、行动提交和生命周期收尾的边界规则。
 *
 * 场景类型：格式入口不变量、队伍合法性聚合、行动提交结构化错误、濒死胜负事件顺序和离场状态清理 场景。
 * 参考来源类型：公开成熟对战引擎的格式配置、行动队列、战斗结算流程，以及现代对战常见公开规则说明。
 * 这些规则大多不是单个技能或道具的效果，而是战斗系统的骨架约束：格式快照必须在启动前自洽；准备阶段要
 * 一次性返回全部队伍违规；行动提交只拦截选择前已经确定非法的输入；真正结算后，濒死、胜负和退场清理必须
 * 以稳定事件顺序暴露给 replay、管理端和后续公开 场景。
 *
 * 验证重点：每个边界都给出稳定 code、异常消息片段或事件顺序断言，避免后续新增技能/特性/道具规则时破坏
 * 战斗入口和生命周期的基本不变量。
 */
class BattleFormatLifecycleBoundaryPublicReferenceTests {
	private val engine = BattleEngine()
	private val preparationValidator = BattlePreparationValidator()
	private val actionValidator = BattleActionValidator()

	@Test
	fun `format rejects blank code`() {
		val scenario = scenario(
			name = "format-rejects-blank-code",
			inputSummary = "格式快照使用空白 code 创建单打格式。",
			expectedSummary = "格式入口立即拒绝空白 code，避免后续 scenario 和 replay 失去稳定格式标识。",
		)

		val failure = assertFailsWith<IllegalArgumentException> {
			BattleFormatSnapshot(code = " ", mode = BattleMode.SINGLE, activeParticipantsPerSide = 1)
		}

		scenario.assertNamed("format-rejects-blank-code")
		assertTrue(failure.message.orEmpty().contains("format code"))
	}

	@Test
	fun `format rejects default level below one`() {
		val scenario = scenario(
			name = "format-rejects-default-level-below-one",
			inputSummary = "格式声明默认等级为 0。",
			expectedSummary = "格式入口拒绝不在 1..100 范围内的默认等级。",
		)

		val failure = assertFailsWith<IllegalArgumentException> {
			BattleFormatSnapshot(
				code = "bad-default-level",
				mode = BattleMode.SINGLE,
				activeParticipantsPerSide = 1,
				defaultLevel = 0,
			)
		}

		scenario.assertNamed("format-rejects-default-level-below-one")
		assertTrue(failure.message.orEmpty().contains("defaultLevel"))
	}

	@Test
	fun `format rejects zero max turns`() {
		val scenario = scenario(
			name = "format-rejects-zero-max-turns",
			inputSummary = "格式声明最大回合数为 0。",
			expectedSummary = "格式入口拒绝非正数最大回合，回合上限必须至少允许完整结算一回合。",
		)

		val failure = assertFailsWith<IllegalArgumentException> {
			BattleFormatSnapshot(
				code = "bad-max-turns",
				mode = BattleMode.SINGLE,
				activeParticipantsPerSide = 1,
				maxTurns = 0,
			)
		}

		scenario.assertNamed("format-rejects-zero-max-turns")
		assertTrue(failure.message.orEmpty().contains("maxTurns"))
	}

	@Test
	fun `initial state rejects duplicate side ids`() {
		val scenario = scenario(
			name = "initial-state-rejects-duplicate-side-ids",
			inputSummary = "初始战斗快照包含两个 sideId 相同的阵营。",
			expectedSummary = "启动入口拒绝重复 sideId，保证事件流和胜负判定可以稳定定位阵营。",
		)

		val failure = assertFailsWith<IllegalArgumentException> {
			BattleInitialState(
				format = singleFormat(),
				rules = neutralRules(),
				sides = listOf(
					BattleSide("same-side", listOf("first"), listOf(participant("first", speed = 100))),
					BattleSide("same-side", listOf("second"), listOf(participant("second", speed = 80))),
				),
			)
		}

		scenario.assertNamed("initial-state-rejects-duplicate-side-ids")
		assertTrue(failure.message.orEmpty().contains("side ids"))
	}

	@Test
	fun `initial state rejects active count mismatch`() {
		val scenario = scenario(
			name = "initial-state-rejects-active-count-mismatch",
			inputSummary = "单打格式中一方却声明两名当前上场成员。",
			expectedSummary = "初始快照拒绝与格式站位数量不一致的上场席位。",
		)

		val failure = assertFailsWith<IllegalArgumentException> {
			BattleInitialState(
				format = singleFormat(),
				rules = neutralRules(),
				sides = listOf(
					BattleSide(
						"side-a",
						listOf("first", "reserve"),
						listOf(participant("first", speed = 100), participant("reserve", speed = 90)),
					),
					BattleSide("side-b", listOf("second"), listOf(participant("second", speed = 80))),
				),
			)
		}

		scenario.assertNamed("initial-state-rejects-active-count-mismatch")
		assertTrue(failure.message.orEmpty().contains("active participant count"))
	}

	@Test
	fun `preparation validator collects direct restrictions on one participant`() {
		val scenario = scenario(
			name = "preparation-validator-collects-direct-restrictions-on-one-participant",
			inputSummary = "同一成员同时超过等级上限，且命中禁用成员、技能、特性和道具限制。",
			expectedSummary = "准备校验一次性返回五条直接违规，不在第一条失败后短路。",
		)
		val restrictedSkill = damagingSkill(skillId = 99)
		val state = initialState(
			first = participant("restricted", speed = 100, skill = restrictedSkill, abilityId = 88, itemId = 77)
				.copy(level = 60, creatureId = 150),
			second = participant("opponent", speed = 80),
			rules = neutralRules().copy(
				maxParticipantLevel = 50,
				bannedCreatureIds = setOf(150),
				bannedSkillIds = setOf(99),
				bannedAbilityIds = setOf(88),
				bannedItemIds = setOf(77),
			),
		)

		val violations = preparationValidator.validate(state)

		scenario.assertNamed("preparation-validator-collects-direct-restrictions-on-one-participant")
		assertEquals(
			listOf("level-too-high", "banned-creature", "banned-skill", "banned-ability", "banned-item"),
			violations.map { it.code },
		)
		assertEquals(listOf(150L, 150L, 99L, 88L, 77L), violations.map { it.resourceId })
	}

	@Test
	fun `preparation validator records duplicate item resource ids`() {
		val scenario = scenario(
			name = "preparation-validator-records-duplicate-item-resource-ids",
			inputSummary = "同一方两名成员携带同一道具，格式启用同队携带道具唯一条款。",
			expectedSummary = "准备校验给两名成员各返回 duplicate-item，并把 resourceId 稳定记录为重复道具 ID。",
		)
		val state = initialState(
			first = participant("item-a", speed = 100, itemId = 10),
			firstBench = listOf(participant("item-b", speed = 90, itemId = 10)),
			second = participant("opponent", speed = 80),
			rules = neutralRules().copy(uniqueItemRequired = true),
		)

		val violations = preparationValidator.validate(state)

		scenario.assertNamed("preparation-validator-records-duplicate-item-resource-ids")
		assertEquals(listOf("duplicate-item", "duplicate-item"), violations.map { it.code })
		assertEquals(listOf(10L, 10L), violations.map { it.resourceId })
	}

	@Test
	fun `preparation validator ignores duplicate clauses when disabled`() {
		val scenario = scenario(
			name = "preparation-validator-ignores-duplicate-clauses-when-disabled",
			inputSummary = "同一方两名成员种类和携带道具都重复，但格式没有启用唯一种类或唯一道具条款。",
			expectedSummary = "准备校验不返回 duplicate-creature 或 duplicate-item，重复条款必须由格式显式启用。",
		)
		val state = initialState(
			first = participant("duplicate-a", speed = 100, itemId = 10).copy(creatureId = 20),
			firstBench = listOf(participant("duplicate-b", speed = 90, itemId = 10).copy(creatureId = 20)),
			second = participant("opponent", speed = 80),
			rules = neutralRules().copy(uniqueCreatureRequired = false, uniqueItemRequired = false),
		)

		val violations = preparationValidator.validate(state)

		scenario.assertNamed("preparation-validator-ignores-duplicate-clauses-when-disabled")
		assertEquals(emptyList(), violations)
	}

	@Test
	fun `action validator reports missing actor`() {
		val scenario = scenario(
			name = "action-validator-reports-missing-actor",
			inputSummary = "提交行动的 actorId 不存在于当前战斗快照。",
			expectedSummary = "行动提交校验返回 actor-not-found，并停止继续检查该行动的技能和目标。",
		)
		val state = engine.start(initialState())

		val violations = actionValidator.validate(
			state,
			listOf(BattleAction.UseSkill("missing", skillId = 1, targetActorId = "side-b-active")),
		)

		scenario.assertNamed("action-validator-reports-missing-actor")
		assertEquals(listOf("actor-not-found"), violations.map { it.code })
		assertEquals("missing", violations.single().actorId)
	}

	@Test
	fun `action validator reports bench actor is not active`() {
		val scenario = scenario(
			name = "action-validator-reports-bench-actor-is-not-active",
			inputSummary = "后备成员在未替换上场前直接提交技能行动。",
			expectedSummary = "行动提交校验返回 actor-not-active，后备成员不能直接进入技能结算。",
		)
		val state = engine.start(
			initialState(firstBench = listOf(participant("bench", speed = 90))),
		)

		val violations = actionValidator.validate(
			state,
			listOf(BattleAction.UseSkill("bench", skillId = 1, targetActorId = "side-b-active")),
		)

		scenario.assertNamed("action-validator-reports-bench-actor-is-not-active")
		assertEquals(listOf("actor-not-active"), violations.map { it.code })
		assertEquals("bench", violations.single().actorId)
	}

	@Test
	fun `action validator reports fainted active skill user`() {
		val scenario = scenario(
			name = "action-validator-reports-fainted-active-skill-user",
			inputSummary = "当前上场成员已经倒下，却提交普通技能行动。",
			expectedSummary = "行动提交校验返回 actor-fainted；倒下成员只能走强制补位替换流程。",
		)
		val state = engine.start(
			initialState(first = participant("fainted", speed = 100, currentHp = 0)),
		)

		val violations = actionValidator.validate(
			state,
			listOf(BattleAction.UseSkill("fainted", skillId = 1, targetActorId = "side-b-active")),
		)

		scenario.assertNamed("action-validator-reports-fainted-active-skill-user")
		assertEquals(listOf("actor-fainted"), violations.map { it.code })
	}

	@Test
	fun `action validator reports target is not active`() {
		val scenario = scenario(
			name = "action-validator-reports-target-is-not-active",
			inputSummary = "当前上场成员把单体技能指向对手后备成员。",
			expectedSummary = "行动提交校验返回 target-not-active，选择阶段目标必须是当前可选的上场成员。",
		)
		val state = engine.start(
			initialState(secondBench = listOf(participant("bench-target", speed = 70))),
		)

		val violations = actionValidator.validate(
			state,
			listOf(BattleAction.UseSkill("side-a-active", skillId = 1, targetActorId = "bench-target")),
		)

		scenario.assertNamed("action-validator-reports-target-is-not-active")
		assertEquals(listOf("target-not-active"), violations.map { it.code })
		assertEquals("bench-target", violations.single().targetActorId)
	}

	@Test
	fun `action validator reports fainted active target`() {
		val scenario = scenario(
			name = "action-validator-reports-fainted-active-target",
			inputSummary = "当前上场成员把单体技能指向已经倒下但仍占据上场席位的目标。",
			expectedSummary = "行动提交校验返回 target-fainted，避免把已经倒下的目标送入技能命中流程。",
		)
		val state = engine.start(
			initialState(second = participant("fainted-target", speed = 80, currentHp = 0)),
		)

		val violations = actionValidator.validate(
			state,
			listOf(BattleAction.UseSkill("side-a-active", skillId = 1, targetActorId = "fainted-target")),
		)

		scenario.assertNamed("action-validator-reports-fainted-active-target")
		assertEquals(listOf("target-fainted"), violations.map { it.code })
		assertEquals("fainted-target", violations.single().targetActorId)
	}

	@Test
	fun `action validator reports switch target already active`() {
		val scenario = scenario(
			name = "action-validator-reports-switch-target-already-active",
			inputSummary = "双打中仍可战斗的一号位提交替换，但替换目标是同侧已经在场的二号位。",
			expectedSummary = "行动提交校验返回 switch-target-active，主动替换必须选择同侧未上场且可战斗成员。",
		)
		val state = engine.start(doubleInitialState())

		val violations = actionValidator.validate(
			state,
			listOf(BattleAction.SwitchParticipant("side-a-active-1", targetActorId = "side-a-active-2")),
		)

		scenario.assertNamed("action-validator-reports-switch-target-already-active")
		assertEquals(listOf("switch-target-active"), violations.map { it.code })
	}

	@Test
	fun `battle ended validation keeps specific action violations`() {
		val scenario = scenario(
			name = "battle-ended-validation-keeps-specific-action-violations",
			inputSummary = "战斗已经因最后一名对手倒下而结束，之后又提交指向该倒下目标的技能行动。",
			expectedSummary = "行动提交校验先返回 battle-ended，同时保留 target-fainted 这类可定位的具体问题。",
		)
		val started = engine.start(
			initialState(
				first = participant("winner", speed = 100),
				second = participant("last-target", speed = 50, currentHp = 20),
			),
		)
		val ended = engine.resolveTurn(
			started,
			listOf(BattleAction.UseSkill("winner", skillId = 1, targetActorId = "last-target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		val violations = actionValidator.validate(
			ended,
			listOf(BattleAction.UseSkill("winner", skillId = 1, targetActorId = "last-target")),
		)

		scenario.assertNamed("battle-ended-validation-keeps-specific-action-violations")
		assertEquals(listOf("battle-ended", "target-fainted"), violations.map { it.code })
	}

	@Test
	fun `participant faint event precedes battle ended event`() {
		val scenario = scenario(
			name = "participant-faint-event-precedes-battle-ended-event",
			inputSummary = "最后一名对手被普通伤害技能击倒。",
			expectedSummary = "事件流必须先记录目标濒死，再记录战斗结束，便于 replay 解释胜负来源。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant("last-target", speed = 50, currentHp = 20),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "last-target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val faintIndex = resolved.events.indexOfFirst { it is BattleEvent.ParticipantFainted }
		val endedIndex = resolved.events.indexOfFirst { it is BattleEvent.BattleEnded }

		scenario.assertNamed("participant-faint-event-precedes-battle-ended-event")
		assertTrue(faintIndex >= 0)
		assertTrue(endedIndex > faintIndex)
	}

	@Test
	fun `battle ending turn does not append normal turn ended`() {
		val scenario = scenario(
			name = "battle-ending-turn-does-not-append-normal-turn-ended",
			inputSummary = "技能伤害在本回合中直接击倒对手最后一名成员。",
			expectedSummary = "战斗结束后不再追加普通 TurnEnded 事件，避免终局回合被误当成可继续回合。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant("last-target", speed = 50, currentHp = 20),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "last-target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("battle-ending-turn-does-not-append-normal-turn-ended")
		assertEquals("all-opponents-fainted", resolved.result?.reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.TurnEnded>())
	}

	@Test
	fun `simultaneous last participants faint produces draw result`() {
		val scenario = scenario(
			name = "simultaneous-last-participants-faint-produces-draw-result",
			inputSummary = "使用者以自身当前 HP 等量伤害击倒目标，同时自身也因技能效果倒下。",
			expectedSummary = "双方最后成员同时倒下时，战斗结果没有胜方并记录 all-sides-fainted。",
		)
		val skill = damagingSkill(
			skillId = 515,
			name = "同归打击",
			elementId = 2,
			damageClass = BattleDamageClass.SPECIAL,
			power = null,
			hpDerivedDamage = BattleHpDerivedDamage.UserCurrentHpAndUserFaints,
		)
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, currentHp = 73, elementId = 2, skill = skill),
				second = participant("target", speed = 80, currentHp = 73),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = 515, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("simultaneous-last-participants-faint-produces-draw-result")
		assertNull(resolved.result?.winningSideId)
		assertEquals("all-sides-fainted", resolved.result?.reason)
		assertEquals(listOf("target", "user"), resolved.events.filterIsInstance<BattleEvent.ParticipantFainted>().map { it.actorId })
	}

	@Test
	fun `switch out clears disable taunt torment and heal block`() {
		val scenario = scenario(
			name = "switch-out-clears-disable-taunt-torment-and-heal-block",
			inputSummary = "成员带着回复封锁、挑衅、定身法和无理取闹状态主动离场。",
			expectedSummary = "离场后这些临时状态全部清除，后续再次上场不会继续继承行动选择限制。",
		)
		val starter = participant("starter", speed = 100).copy(
			healBlockTurnsRemaining = 2,
			tauntTurnsRemaining = 2,
			disabledSkillId = 1,
			disabledSkillTurnsRemaining = 2,
			tormented = true,
		)
		val state = engine.start(
			initialState(
				first = starter,
				firstBench = listOf(participant("reserve", speed = 90)),
				second = participant("observer", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("starter", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)
		val switchedOut = resolved.participant("starter")

		scenario.assertNamed("switch-out-clears-disable-taunt-torment-and-heal-block")
		assertEquals(0, switchedOut?.healBlockTurnsRemaining)
		assertEquals(0, switchedOut?.tauntTurnsRemaining)
		assertNull(switchedOut?.disabledSkillId)
		assertEquals(0, switchedOut?.disabledSkillTurnsRemaining)
		assertEquals(false, switchedOut?.tormented)
	}

	@Test
	fun `forced switch clears binding locked choice and substitute state`() {
		val scenario = scenario(
			name = "forced-switch-clears-binding-locked-choice-and-substitute-state",
			inputSummary = "已经倒下的当前上场成员仍带着束缚、锁招、讲究锁定、最近成功技能和替身运行态。",
			expectedSummary = "强制补位离场会统一清除这些只应保留在上场期间的运行态。",
		)
		val fainted = participant("fainted-active", speed = 100, currentHp = 0).copy(
			boundByActorId = "observer",
			bindingTurnsRemaining = 2,
			lastSuccessfulSkillId = 1,
			lockedMoveSkillId = 1,
			lockedMoveTargetActorId = "observer",
			lockedMoveTurnsRemaining = 1,
			choiceLockedSkillId = 1,
			substituteHp = 25,
		)
		val state = engine.start(
			initialState(
				first = fainted,
				firstBench = listOf(participant("reserve", speed = 90)),
				second = participant("observer", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("fainted-active", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)
		val switchedOut = resolved.participant("fainted-active")

		scenario.assertNamed("forced-switch-clears-binding-locked-choice-and-substitute-state")
		assertNull(switchedOut?.boundByActorId)
		assertEquals(0, switchedOut?.bindingTurnsRemaining)
		assertNull(switchedOut?.lastSuccessfulSkillId)
		assertNull(switchedOut?.lockedMoveSkillId)
		assertNull(switchedOut?.lockedMoveTargetActorId)
		assertEquals(0, switchedOut?.lockedMoveTurnsRemaining)
		assertNull(switchedOut?.choiceLockedSkillId)
		assertEquals(0, switchedOut?.substituteHp)
	}

	private fun scenario(
		name: String,
		inputSummary: String,
		expectedSummary: String,
	): PublicBattleRuleScenario =
		publicBattleRuleScenario(
			name = name,
			inputSummary = inputSummary,
			expectedSummary = expectedSummary,
		)
}
