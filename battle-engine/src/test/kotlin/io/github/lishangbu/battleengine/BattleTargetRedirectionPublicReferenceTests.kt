package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证现代主系列目标槽位重定向和空目标范围。
 *
 * 场景类型：单体目标槽位、双打选中目标、范围技能忽略提交目标、目标为空时取消行动 fixture。
 * 参考来源类型：公开成熟对战引擎的行动结算和目标解析实现，以及公开技能目标说明。
 * 现代规则里，玩家提交的是“技能 + 目标槽位”；单体技能会在执行时读取该槽位当前成员，范围技能则按技能
 * 目标范围重新收集目标。若最终没有任何可战斗目标，行动在使用前取消，不消耗 PP，也不产生技能使用事件。
 * 验证重点：目标重定向不能跨槽位乱跳；范围技能不能被提交目标参数缩小；保护等后续阻挡不改变范围目标倍率。
 */
class BattleTargetRedirectionPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `selected target follows single battle replacement slot`() {
		val fixture = publicBattleRuleFixture(
			name = "selected-target-follows-single-battle-replacement-slot",
			inputSummary = "单打中目标先主动替换，攻击方技能仍指向原目标 actorId。",
			expectedSummary = "技能按目标槽位命中新上场成员，原离场成员不受伤害。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 60),
				second = participant("starter", speed = 100),
				secondBench = listOf(participant("reserve", speed = 80)),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.SwitchParticipant("starter", targetActorId = "reserve"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "starter"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("selected-target-follows-single-battle-replacement-slot")
		assertEquals(100, resolved.participant("starter")?.currentHp)
		assertEquals(72, resolved.participant("reserve")?.currentHp)
		assertEquals(listOf("reserve"), resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.targetActorId })
	}

	@Test
	fun `selected target in double follows replaced selected slot only`() {
		val fixture = publicBattleRuleFixture(
			name = "selected-target-in-double-follows-replaced-selected-slot-only",
			inputSummary = "双打中对方左侧目标替换，右侧目标保持在场；攻击方选择左侧槽位。",
			expectedSummary = "单体技能只命中左侧槽位的新上场成员，不会改打右侧在场成员。",
		)
		val state = engine.start(
			BattleInitialState(
				format = doubleFormat(),
				rules = neutralRules(),
				sides = listOf(
					BattleSide(
						sideId = "side-a",
						activeActorIds = listOf("attacker", "ally"),
						participants = listOf(participant("attacker", speed = 100), participant("ally", speed = 90)),
					),
					BattleSide(
						sideId = "side-b",
						activeActorIds = listOf("target-left", "target-right"),
						participants = listOf(
							participant("target-left", speed = 80),
							participant("target-right", speed = 70),
							participant("reserve", speed = 60),
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.SwitchParticipant("target-left", targetActorId = "reserve"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "target-left"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("selected-target-in-double-follows-replaced-selected-slot-only")
		assertEquals(72, resolved.participant("reserve")?.currentHp)
		assertEquals(100, resolved.participant("target-right")?.currentHp)
		assertEquals(listOf("reserve"), resolved.events.filterIsInstance<BattleEvent.DamageApplied>().map { it.targetActorId })
	}

	@Test
	fun `selected target fainted current slot does not redirect to other slot`() {
		val fixture = publicBattleRuleFixture(
			name = "selected-target-fainted-current-slot-does-not-redirect-to-other-slot",
			inputSummary = "双打中选择的目标槽位当前成员已经无法战斗，另一名对手仍在场。",
			expectedSummary = "单体技能不会重定向到另一槽位；行动在使用前取消且不消耗 PP。",
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("attacker", speed = 100),
				firstB = participant("ally", speed = 90),
				secondA = participant("fainted-target", speed = 80, currentHp = 0),
				secondB = participant("other-target", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "fainted-target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("selected-target-fainted-current-slot-does-not-redirect-to-other-slot")
		assertEquals(35, resolved.participant("attacker")?.skillSlot(1)?.remainingPp)
		assertEquals(100, resolved.participant("other-target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillUsed>())
	}

	@Test
	fun `selected target with no capable active member cancels before pp`() {
		val fixture = publicBattleRuleFixture(
			name = "selected-target-with-no-capable-active-member-cancels-before-pp",
			inputSummary = "单打中目标侧当前没有可战斗上场成员。",
			expectedSummary = "找不到可战斗目标时行动在技能使用前取消，不消耗 PP。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant("fainted-target", speed = 50, currentHp = 0),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "fainted-target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("selected-target-with-no-capable-active-member-cancels-before-pp")
		assertEquals(35, resolved.participant("attacker")?.skillSlot(1)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillUsed>())
	}

	@Test
	fun `all adjacent opponents with no capable opponents cancels before pp`() {
		val fixture = publicBattleRuleFixture(
			name = "all-adjacent-opponents-with-no-capable-opponents-cancels-before-pp",
			inputSummary = "双打中范围为全体相邻对手的技能执行时，对手两名上场成员都无法战斗。",
			expectedSummary = "范围目标集合为空，行动在技能使用前取消且不消耗 PP。",
		)
		val spreadSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("spread-user", speed = 100, skill = spreadSkill),
				firstB = participant("ally", speed = 90),
				secondA = participant("fainted-left", speed = 80, currentHp = 0),
				secondB = participant("fainted-right", speed = 70, currentHp = 0),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("spread-user", skillId = 1, targetActorId = "fainted-left")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("all-adjacent-opponents-with-no-capable-opponents-cancels-before-pp")
		assertEquals(35, resolved.participant("spread-user")?.skillSlot(1)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillUsed>())
	}

	@Test
	fun `all adjacent participants with only user capable cancels before pp`() {
		val fixture = publicBattleRuleFixture(
			name = "all-adjacent-participants-with-only-user-capable-cancels-before-pp",
			inputSummary = "双打中全体相邻成员范围技能执行时，除使用者外没有可战斗成员。",
			expectedSummary = "目标集合为空，行动在技能使用前取消，不消耗 PP。",
		)
		val areaSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("area-user", speed = 100, skill = areaSkill),
				firstB = participant("fainted-ally", speed = 90, currentHp = 0),
				secondA = participant("fainted-left", speed = 80, currentHp = 0),
				secondB = participant("fainted-right", speed = 70, currentHp = 0),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("area-user", skillId = 1, targetActorId = "fainted-left")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("all-adjacent-participants-with-only-user-capable-cancels-before-pp")
		assertEquals(35, resolved.participant("area-user")?.skillSlot(1)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillUsed>())
	}

	@Test
	fun `all adjacent opponents ignores submitted target actor`() {
		val fixture = publicBattleRuleFixture(
			name = "all-adjacent-opponents-ignores-submitted-target-actor",
			inputSummary = "双打中全体相邻对手范围技能提交的 targetActorId 指向同侧伙伴。",
			expectedSummary = "范围技能忽略提交目标 actorId，仍命中所有可战斗对手且不命中伙伴。",
		)
		val spreadSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("spread-user", speed = 100, skill = spreadSkill),
				firstB = participant("ally", speed = 90),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("spread-user", skillId = 1, targetActorId = "ally")),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		fixture.assertNamed("all-adjacent-opponents-ignores-submitted-target-actor")
		assertEquals(
			listOf("opponent-left", "opponent-right"),
			resolved.events.filterIsInstance<BattleEvent.DamageApplied>().map { it.targetActorId },
		)
		assertEquals(100, resolved.participant("ally")?.currentHp)
	}

	@Test
	fun `all adjacent participants ignores selected target and includes ally`() {
		val fixture = publicBattleRuleFixture(
			name = "all-adjacent-participants-ignores-selected-target-and-includes-ally",
			inputSummary = "双打中全体相邻成员范围技能提交的 targetActorId 指向使用者自身。",
			expectedSummary = "范围技能忽略提交目标 actorId，命中伙伴和两个对手，但排除使用者自身。",
		)
		val areaSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("area-user", speed = 100, skill = areaSkill),
				firstB = participant("ally", speed = 90),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("area-user", skillId = 1, targetActorId = "area-user")),
			ScriptedBattleRandom(listOf(1, 15, 1, 15, 1, 15)),
		)

		fixture.assertNamed("all-adjacent-participants-ignores-selected-target-and-includes-ally")
		assertEquals(
			listOf("ally", "opponent-left", "opponent-right"),
			resolved.events.filterIsInstance<BattleEvent.DamageApplied>().map { it.targetActorId },
		)
		assertEquals(100, resolved.participant("area-user")?.currentHp)
	}

	@Test
	fun `spread target multiplier remains when one target protects`() {
		val fixture = publicBattleRuleFixture(
			name = "spread-target-multiplier-remains-when-one-target-protects",
			inputSummary = "双打中范围技能有两个可战斗目标，其中一个目标本回合先使用保护。",
			expectedSummary = "保护目标不受伤害，但另一目标的伤害事件仍使用多目标范围倍率。",
		)
		val spreadSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("spread-user", speed = 100, skill = spreadSkill),
				firstB = participant("ally", speed = 90),
				secondA = participant("protector", speed = 80, skill = protectionSkill()),
				secondB = participant("other-target", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "spread-user"),
				BattleAction.UseSkill("spread-user", skillId = 1, targetActorId = "protector"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		fixture.assertNamed("spread-target-multiplier-remains-when-one-target-protects")
		assertEquals("protector", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single().targetActorId)
		assertEquals("other-target", damage.targetActorId)
		assertEquals(0.75, damage.targetMultiplier)
		assertEquals(100, resolved.participant("protector")?.currentHp)
	}

	@Test
	fun `spread target order follows current active slot order after switch`() {
		val fixture = publicBattleRuleFixture(
			name = "spread-target-order-follows-current-active-slot-order-after-switch",
			inputSummary = "双打中对方左侧成员先替换，随后范围技能重新收集两个对手目标。",
			expectedSummary = "伤害事件顺序按当前 active slot 顺序记录，新上场成员位于原左侧槽位。",
		)
		val spreadSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		val state = engine.start(
			BattleInitialState(
				format = doubleFormat(),
				rules = neutralRules(),
				sides = listOf(
					BattleSide(
						sideId = "side-a",
						activeActorIds = listOf("spread-user", "ally"),
						participants = listOf(
							participant("spread-user", speed = 100, skill = spreadSkill),
							participant("ally", speed = 90),
						),
					),
					BattleSide(
						sideId = "side-b",
						activeActorIds = listOf("left", "right"),
						participants = listOf(
							participant("left", speed = 80),
							participant("right", speed = 70),
							participant("reserve", speed = 60),
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.SwitchParticipant("left", targetActorId = "reserve"),
				BattleAction.UseSkill("spread-user", skillId = 1, targetActorId = "left"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		fixture.assertNamed("spread-target-order-follows-current-active-slot-order-after-switch")
		assertEquals(
			listOf("reserve", "right"),
			resolved.events.filterIsInstance<BattleEvent.DamageApplied>().map { it.targetActorId },
		)
		assertEquals(listOf("reserve", "right"), resolved.sideOf("reserve")?.activeActorIds)
	}
}
