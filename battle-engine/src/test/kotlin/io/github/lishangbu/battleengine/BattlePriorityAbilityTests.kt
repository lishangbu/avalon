package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证当前上场特性对对手先制技能的阻挡。
 *
 * 场景类型：目标前置条件 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。这类特性保护拥有者所在一侧，阻止对手先制技能影响自身或
 * 同侧伙伴；技能使用本身仍消耗 PP，但不会继续进入命中、伤害或附加效果流程。同侧成员使用先制技能不受阻挡。
 */
class BattlePriorityAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `priority blocking ability blocks opponent priority move against holder`() {
		val fixture = publicBattleRuleFixture(
			name = "priority-blocking-ability-blocks-opponent-priority-move-against-holder",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Queenly_Majesty_(Ability)",
			),
			inputSummary = "目标当前上场并拥有阻止对手先制技能影响己方的结构化特性，对手使用先制攻击指定该目标。",
			expectedSummary = "技能消耗 PP 后被目标特性阻挡，不产生命中、伤害或附加效果。",
		)
		val prioritySkill = damagingSkill(name = "先制测试", priority = 1)
		val state = engine.start(
			initialState(
				first = participant("priority-user", speed = 50, skill = prioritySkill),
				second = participant(
					"ability-holder",
					speed = 100,
					abilityId = 214,
					abilityEffects = listOf(BattleAbilityEffect.PriorityMoveImmunityForSide()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("priority-user", skillId = 1, targetActorId = "ability-holder")),
			ScriptedBattleRandom(emptyList()),
		)
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>().single()

		fixture.assertNamed("priority-blocking-ability-blocks-opponent-priority-move-against-holder")
		assertEquals(34, resolved.participant("priority-user")?.skillSlot(1)?.remainingPp)
		assertEquals(100, resolved.participant("ability-holder")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals("ability-holder", blocked.abilityHolderActorId)
		assertEquals(214, blocked.abilityId)
	}

	@Test
	fun `priority blocking ability protects active ally from opponent priority move`() {
		val fixture = publicBattleRuleFixture(
			name = "priority-blocking-ability-protects-active-ally-from-opponent-priority-move",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Dazzling_(Ability)",
				"https://bulbapedia.bulbagarden.net/wiki/Armor_Tail_(Ability)",
			),
			inputSummary = "双打中目标本身没有阻挡特性，但同侧另一个当前上场成员拥有先制阻挡特性，对手使用先制攻击指定目标。",
			expectedSummary = "技能消耗 PP 后被伙伴特性阻挡，目标不受到伤害，事件记录实际特性拥有者。",
		)
		val prioritySkill = damagingSkill(name = "先制测试", priority = 1)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("priority-user", speed = 50, skill = prioritySkill),
				firstB = participant("observer", speed = 40),
				secondA = participant("protected-ally", speed = 100),
				secondB = participant(
					"ability-holder",
					speed = 90,
					abilityId = 219,
					abilityEffects = listOf(BattleAbilityEffect.PriorityMoveImmunityForSide()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("priority-user", skillId = 1, targetActorId = "protected-ally")),
			ScriptedBattleRandom(emptyList()),
		)
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>().single()

		fixture.assertNamed("priority-blocking-ability-protects-active-ally-from-opponent-priority-move")
		assertEquals(100, resolved.participant("protected-ally")?.currentHp)
		assertEquals("ability-holder", blocked.abilityHolderActorId)
		assertEquals(219, blocked.abilityId)
		assertEquals("protected-ally", blocked.targetActorId)
	}

	@Test
	fun `priority blocking ability does not block ally priority move`() {
		val fixture = publicBattleRuleFixture(
			name = "priority-blocking-ability-does-not-block-ally-priority-move",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Queenly_Majesty_(Ability)",
			),
			inputSummary = "双打中同侧成员对拥有先制阻挡特性的伙伴使用先制攻击。",
			expectedSummary = "特性只阻止对手先制技能，不阻止同侧目标；技能按普通伤害流程结算。",
		)
		val prioritySkill = damagingSkill(name = "同侧先制测试", priority = 1)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("priority-user", speed = 50, skill = prioritySkill),
				firstB = participant(
					"ability-holder",
					speed = 100,
					abilityId = 214,
					abilityEffects = listOf(BattleAbilityEffect.PriorityMoveImmunityForSide()),
				),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("priority-user", skillId = 1, targetActorId = "ability-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("priority-blocking-ability-does-not-block-ally-priority-move")
		assertEquals(72, resolved.participant("ability-holder")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>())
		assertEquals("ability-holder", resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().targetActorId)
	}
}
