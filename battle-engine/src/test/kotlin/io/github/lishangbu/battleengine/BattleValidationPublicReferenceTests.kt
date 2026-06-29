package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证准备阶段和行动提交阶段的公开规则边界。
 *
 * 场景类型：开局前队伍合法性、回合行动提交合法性 fixture。
 * 参考来源类型：公开成熟对战引擎的规则集与行动队列实现，以及现代对战常见条款说明。战斗引擎真正结算前，
 * 服务端必须先拦截明显非法的队伍和行动：等级上限、禁用资源、重复种类、重复道具、同一成员重复行动、
 * PP 耗尽和讲究类技能锁定。这些规则不应该在伤害、命中或状态机中用异常文本临时兜底，而应返回稳定 code，
 * 让接口、管理端和未来匹配服务都能复用同一套校验结果。
 * 验证重点：准备阶段按单方队伍维度聚合违规；行动提交阶段只拦截选择前就确定非法的行为；返回 code 与
 * 资源 ID 稳定，便于公开 fixture、管理端展示和回放记录长期对齐。
 */
class BattleValidationPublicReferenceTests {
	private val preparationValidator = BattlePreparationValidator()
	private val actionValidator = BattleActionValidator()
	private val engine = BattleEngine()

	@Test
	fun `level cap rejects over level participant`() {
		val fixture = publicBattleRuleFixture(
			name = "level-cap-rejects-over-level-participant",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/config/formats.ts",
				"https://wiki.52poke.com/wiki/对战设施",
			),
			inputSummary = "规则快照声明等级上限 50，队伍中一名成员等级为 60。",
			expectedSummary = "准备阶段校验返回 level-too-high，违规定位到该成员并记录其成员种类 ID。",
		)
		val state = initialState(
			first = participant("over-level", speed = 100).copy(level = 60, creatureId = 25),
			second = participant("opponent", speed = 80),
			rules = neutralRules().copy(maxParticipantLevel = 50),
		)

		val violations = preparationValidator.validate(state)

		fixture.assertNamed("level-cap-rejects-over-level-participant")
		assertEquals(listOf("level-too-high"), violations.map { it.code })
		assertEquals("over-level", violations.single().actorId)
		assertEquals(25, violations.single().resourceId)
	}

	@Test
	fun `banned creature rule rejects restricted creature`() {
		val fixture = publicBattleRuleFixture(
			name = "banned-creature-rule-rejects-restricted-creature",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/config/formats.ts",
				"https://wiki.52poke.com/wiki/规则",
			),
			inputSummary = "规则快照禁用成员种类 150，队伍中一名成员的种类 ID 为 150。",
			expectedSummary = "准备阶段校验返回 banned-creature，并把资源 ID 记录为被禁用的成员种类 ID。",
		)
		val state = initialState(
			first = participant("restricted-creature", speed = 100).copy(creatureId = 150),
			second = participant("opponent", speed = 80),
			rules = neutralRules().copy(bannedCreatureIds = setOf(150)),
		)

		val violations = preparationValidator.validate(state)

		fixture.assertNamed("banned-creature-rule-rejects-restricted-creature")
		assertEquals(listOf("banned-creature"), violations.map { it.code })
		assertEquals(150, violations.single().resourceId)
	}

	@Test
	fun `banned skill rule rejects restricted skill`() {
		val fixture = publicBattleRuleFixture(
			name = "banned-skill-rule-rejects-restricted-skill",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/config/formats.ts",
				"https://wiki.52poke.com/wiki/招式",
			),
			inputSummary = "规则快照禁用技能 99，队伍成员携带技能 99。",
			expectedSummary = "准备阶段校验返回 banned-skill，并把资源 ID 记录为被禁用技能 ID。",
		)
		val state = initialState(
			first = participant("restricted-skill", speed = 100, skill = damagingSkill(skillId = 99)),
			second = participant("opponent", speed = 80),
			rules = neutralRules().copy(bannedSkillIds = setOf(99)),
		)

		val violations = preparationValidator.validate(state)

		fixture.assertNamed("banned-skill-rule-rejects-restricted-skill")
		assertEquals(listOf("banned-skill"), violations.map { it.code })
		assertEquals(99, violations.single().resourceId)
	}

	@Test
	fun `banned ability rule rejects restricted ability`() {
		val fixture = publicBattleRuleFixture(
			name = "banned-ability-rule-rejects-restricted-ability",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/config/formats.ts",
				"https://wiki.52poke.com/wiki/特性",
			),
			inputSummary = "规则快照禁用特性 88，队伍成员当前特性 ID 为 88。",
			expectedSummary = "准备阶段校验返回 banned-ability，并把资源 ID 记录为被禁用特性 ID。",
		)
		val state = initialState(
			first = participant("restricted-ability", speed = 100, abilityId = 88),
			second = participant("opponent", speed = 80),
			rules = neutralRules().copy(bannedAbilityIds = setOf(88)),
		)

		val violations = preparationValidator.validate(state)

		fixture.assertNamed("banned-ability-rule-rejects-restricted-ability")
		assertEquals(listOf("banned-ability"), violations.map { it.code })
		assertEquals(88, violations.single().resourceId)
	}

	@Test
	fun `banned item rule rejects restricted held item`() {
		val fixture = publicBattleRuleFixture(
			name = "banned-item-rule-rejects-restricted-held-item",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/config/formats.ts",
				"https://wiki.52poke.com/wiki/道具",
			),
			inputSummary = "规则快照禁用道具 77，队伍成员携带道具 ID 为 77。",
			expectedSummary = "准备阶段校验返回 banned-item，并把资源 ID 记录为被禁用道具 ID。",
		)
		val state = initialState(
			first = participant("restricted-item", speed = 100, itemId = 77),
			second = participant("opponent", speed = 80),
			rules = neutralRules().copy(bannedItemIds = setOf(77)),
		)

		val violations = preparationValidator.validate(state)

		fixture.assertNamed("banned-item-rule-rejects-restricted-held-item")
		assertEquals(listOf("banned-item"), violations.map { it.code })
		assertEquals(77, violations.single().resourceId)
	}

	@Test
	fun `unique creature clause rejects duplicates only inside one side`() {
		val fixture = publicBattleRuleFixture(
			name = "unique-creature-clause-rejects-duplicates-only-inside-one-side",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/config/formats.ts",
				"https://wiki.52poke.com/wiki/规则",
			),
			inputSummary = "规则快照要求同队成员种类唯一，一方队伍内两名成员种类相同，对手也有同种类成员。",
			expectedSummary = "准备阶段只报告同一方内部的 duplicate-creature；不同阵营之间的相同种类不违规。",
		)
		val duplicateA = participant("duplicate-a", speed = 100).copy(creatureId = 20)
		val duplicateB = participant("duplicate-b", speed = 80).copy(creatureId = 20)
		val opponent = participant("opponent-same-creature", speed = 70).copy(creatureId = 20)
		val state = initialState(
			first = duplicateA,
			firstBench = listOf(duplicateB),
			second = opponent,
			rules = neutralRules().copy(uniqueCreatureRequired = true),
		)

		val violations = preparationValidator.validate(state)

		fixture.assertNamed("unique-creature-clause-rejects-duplicates-only-inside-one-side")
		assertEquals(listOf("duplicate-a", "duplicate-b"), violations.map { it.actorId })
		assertEquals(listOf("duplicate-creature", "duplicate-creature"), violations.map { it.code })
	}

	@Test
	fun `unique item clause rejects duplicate held items only inside one side`() {
		val fixture = publicBattleRuleFixture(
			name = "unique-item-clause-rejects-duplicate-held-items-only-inside-one-side",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/config/formats.ts",
				"https://wiki.52poke.com/wiki/道具",
			),
			inputSummary = "规则快照要求同队携带道具唯一，一方队伍内两名成员携带同一道具，对手也携带同一道具。",
			expectedSummary = "准备阶段只报告同一方内部的 duplicate-item；不同阵营之间的相同道具不违规。",
		)
		val duplicateA = participant("item-a", speed = 100, itemId = 10)
		val duplicateB = participant("item-b", speed = 80, itemId = 10)
		val opponent = participant("opponent-same-item", speed = 70, itemId = 10)
		val state = initialState(
			first = duplicateA,
			firstBench = listOf(duplicateB),
			second = opponent,
			rules = neutralRules().copy(uniqueItemRequired = true),
		)

		val violations = preparationValidator.validate(state)

		fixture.assertNamed("unique-item-clause-rejects-duplicate-held-items-only-inside-one-side")
		assertEquals(listOf("item-a", "item-b"), violations.map { it.actorId })
		assertEquals(listOf("duplicate-item", "duplicate-item"), violations.map { it.code })
	}

	@Test
	fun `duplicate action submission reports every duplicated actor action`() {
		val fixture = publicBattleRuleFixture(
			name = "duplicate-action-submission-reports-every-duplicated-actor-action",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-queue.ts",
				"https://wiki.52poke.com/wiki/对战",
			),
			inputSummary = "同一成员在同一回合提交两条技能行动，两条行动本身都指向有效目标。",
			expectedSummary = "行动提交校验为这两条行动都返回 duplicate-action，调用方可以定位重复提交来源。",
		)
		val state = engine.start(initialState())

		val violations = actionValidator.validate(
			state,
			listOf(
				BattleAction.UseSkill("side-a-active", skillId = 1, targetActorId = "side-b-active"),
				BattleAction.UseSkill("side-a-active", skillId = 1, targetActorId = "side-b-active"),
			),
		)

		fixture.assertNamed("duplicate-action-submission-reports-every-duplicated-actor-action")
		assertEquals(listOf("duplicate-action", "duplicate-action"), violations.map { it.code })
		assertEquals(listOf("side-a-active", "side-a-active"), violations.map { it.actorId })
	}

	@Test
	fun `skill with no pp is rejected before engine resolution`() {
		val fixture = publicBattleRuleFixture(
			name = "skill-with-no-pp-is-rejected-before-engine-resolution",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
				"https://wiki.52poke.com/wiki/PP",
			),
			inputSummary = "当前上场成员选择剩余 PP 为 0 的技能，目标成员有效且仍可战斗。",
			expectedSummary = "行动提交校验返回 skill-no-pp；该行动不应进入正式回合状态机。",
		)
		val emptySkill = damagingSkill(skillId = 1).copy(remainingPp = 0, maxPp = 35)
		val state = engine.start(
			initialState(
				first = participant("empty-pp", speed = 100, skill = emptySkill),
				second = participant("target", speed = 80),
			),
		)

		val violations = actionValidator.validate(
			state,
			listOf(BattleAction.UseSkill("empty-pp", skillId = 1, targetActorId = "target")),
		)

		fixture.assertNamed("skill-with-no-pp-is-rejected-before-engine-resolution")
		assertEquals(listOf("skill-no-pp"), violations.map { it.code })
		assertEquals(1, violations.single().resourceId)
	}

	@Test
	fun `choice locked participant cannot submit a different skill`() {
		val fixture = publicBattleRuleFixture(
			name = "choice-locked-participant-cannot-submit-a-different-skill",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
			),
			inputSummary = "成员已经被讲究类效果锁定到技能 2，本回合尝试选择技能 1。",
			expectedSummary = "行动提交校验返回 choice-locked，并把资源 ID 记录为当前锁定的技能 ID。",
		)
		val firstSkill = damagingSkill(skillId = 1, name = "普通攻击")
		val secondSkill = damagingSkill(skillId = 2, name = "锁定技能")
		val state = engine.start(
			initialState(
				first = participant("choice-user", speed = 100, skill = firstSkill).copy(
					skillSlots = listOf(firstSkill, secondSkill),
					choiceLockedSkillId = 2,
				),
				second = participant("target", speed = 80),
			),
		)

		val violations = actionValidator.validate(
			state,
			listOf(BattleAction.UseSkill("choice-user", skillId = 1, targetActorId = "target")),
		)

		fixture.assertNamed("choice-locked-participant-cannot-submit-a-different-skill")
		assertEquals(listOf("choice-locked"), violations.map { it.code })
		assertEquals(2, violations.single().resourceId)
	}
}
