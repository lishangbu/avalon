package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证现代主系列行动流程边界。
 *
 * 场景类型：保护、命中前短路、目标有效性和濒死后行动取消 fixture。
 * 参考来源类型：公开成熟对战引擎的行动队列实现、公开技能资料和公开规则说明。现代规则里，行动已经排队
 * 并不代表一定会消耗 PP 或产生技能使用事件：使用者倒下、离场或没有可战斗目标时会在行动入口直接取消；
 * 保护屏障则发生在技能已经使用并消耗 PP 之后，且位于命中判定之前。少数明确不受保护影响的技能会绕过保护，
 * 但仍要继续执行命中、免疫和伤害流程。
 * 验证重点：保护阻挡与保护绕过的随机消费顺序稳定；濒死成员不会继续执行排队行动；没有可战斗目标时不扣 PP；
 * 这些边界不通过技能名称判断，而由结构化技能字段和当前战斗状态决定。
 */
class BattleActionFlowBoundaryTests {
	private val engine = BattleEngine()

	@Test
	fun `skill unaffected by protection damages protected target`() {
		val fixture = publicBattleRuleFixture(
			name = "protection-bypass-skill-damages-protected-target",
			inputSummary = "目标本回合先建立保护屏障，攻击方随后使用声明为不受保护影响的物理技能。",
			expectedSummary = "技能不会产生保护阻挡事件，会继续进入要害和伤害随机流程并对受保护目标造成伤害。",
		)
		val bypassSkill = damagingSkill(skillId = 3, name = "破防测试", affectedByProtect = false)
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 50, skill = protectionSkill()),
				second = participant("attacker", speed = 100, skill = bypassSkill),
			),
		)
		val random = ScriptedBattleRandom(listOf(1, 15))

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 3, targetActorId = "protector"),
			),
			random,
		)

		fixture.assertNamed("protection-bypass-skill-damages-protected-target")
		assertEquals(72, resolved.participant("protector")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertEquals(listOf("critical hit for 3", "damage random for 3"), random.consumedReasons())
	}

	@Test
	fun `protection block happens before accuracy random`() {
		val fixture = publicBattleRuleFixture(
			name = "protection-blocks-before-accuracy-random",
			inputSummary = "目标本回合先建立保护屏障，攻击方随后使用基础命中 50 且受保护影响的技能。",
			expectedSummary = "攻击方技能消耗 PP 后被保护阻挡，不再消费命中随机数，也不会造成伤害。",
		)
		val inaccurateSkill = damagingSkill(skillId = 4, name = "受阻命中测试", accuracy = 50)
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 50, skill = protectionSkill()),
				second = participant("attacker", speed = 100, skill = inaccurateSkill),
			),
		)
		val random = ScriptedBattleRandom(emptyList())

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 4, targetActorId = "protector"),
			),
			random,
		)

		fixture.assertNamed("protection-blocks-before-accuracy-random")
		assertEquals(100, resolved.participant("protector")?.currentHp)
		assertEquals(34, resolved.participant("attacker")?.skillSlot(4)?.remainingPp)
		assertEquals("protector", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single().targetActorId)
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `protection bypass still performs accuracy check before damage`() {
		val fixture = publicBattleRuleFixture(
			name = "protection-bypass-still-performs-accuracy-check",
			inputSummary = "目标本回合先建立保护屏障，攻击方随后使用不受保护影响但基础命中 50 的技能，命中掷点失败。",
			expectedSummary = "技能绕过保护后仍执行命中判定；未命中时不造成伤害，也不进入要害和伤害随机流程。",
		)
		val bypassSkill = damagingSkill(skillId = 5, name = "绕保护命中测试", accuracy = 50, affectedByProtect = false)
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 50, skill = protectionSkill()),
				second = participant("attacker", speed = 100, skill = bypassSkill),
			),
		)
		val random = ScriptedBattleRandom(listOf(99))

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 5, targetActorId = "protector"),
			),
			random,
		)

		fixture.assertNamed("protection-bypass-still-performs-accuracy-check")
		assertEquals(100, resolved.participant("protector")?.currentHp)
		assertEquals(100, resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single().accuracyRoll)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertEquals(listOf("accuracy for 5"), random.consumedReasons())
	}

	@Test
	fun `fainted queued actor cannot execute later action`() {
		val fixture = publicBattleRuleFixture(
			name = "fainted-queued-actor-cannot-execute-later-action",
			inputSummary = "双打中高速成员先击倒一个低速目标，该目标本回合原本也提交了技能行动。",
			expectedSummary = "低速目标倒下后，其排队行动在执行入口取消；事件流中不会出现该成员的技能使用事件。",
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("fast-attacker", speed = 100),
				firstB = participant("ally", speed = 90),
				secondA = participant("fainted-before-action", speed = 50, currentHp = 20),
				secondB = participant("surviving-partner", speed = 40),
			),
		)
		val random = ScriptedBattleRandom(listOf(1, 15))

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("fast-attacker", skillId = 1, targetActorId = "fainted-before-action"),
				BattleAction.UseSkill("fainted-before-action", skillId = 1, targetActorId = "fast-attacker"),
			),
			random,
		)

		fixture.assertNamed("fainted-queued-actor-cannot-execute-later-action")
		assertEquals(null, resolved.result)
		assertEquals(0, resolved.participant("fainted-before-action")?.currentHp)
		assertEquals(listOf("fast-attacker"), resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId })
		assertEquals(listOf("fainted-before-action"), resolved.events.filterIsInstance<BattleEvent.ParticipantFainted>().map { it.actorId })
	}

	@Test
	fun `action with no battle capable selected target consumes no pp`() {
		val fixture = publicBattleRuleFixture(
			name = "action-with-no-battle-capable-selected-target-consumes-no-pp",
			inputSummary = "双打目标槽位当前成员已经无法战斗，行动方仍提交了指向该成员的单体技能。",
			expectedSummary = "技能阶段找不到可战斗目标时，本次行动在使用前取消，不产生技能使用事件，也不消耗 PP。",
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("attacker", speed = 100),
				firstB = participant("ally", speed = 90),
				secondA = participant("unavailable-target", speed = 50, currentHp = 0),
				secondB = participant("surviving-partner", speed = 40),
			),
		)
		val random = ScriptedBattleRandom(emptyList())

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "unavailable-target")),
			random,
		)

		fixture.assertNamed("action-with-no-battle-capable-selected-target-consumes-no-pp")
		assertEquals(35, resolved.participant("attacker")?.skillSlot(1)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillUsed>())
		assertEquals(emptyList(), random.consumedReasons())
	}
}
