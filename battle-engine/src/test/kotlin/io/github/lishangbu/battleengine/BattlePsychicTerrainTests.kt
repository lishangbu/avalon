package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证精神场地对先制技能的阻挡。
 *
 * 场景类型：场地目标前置条件 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。精神场地只保护当前接地成员，且只阻止对手使用的先制技能；
 * 技能使用本身仍然消耗 PP，但不会继续命中、伤害或附加效果。
 * 验证重点：接地判定和对手侧判定参与阻挡，非接地目标或同侧目标仍按普通技能流程结算。
 */
class BattlePsychicTerrainTests {
	private val engine = BattleEngine()

	@Test
	fun `psychic terrain blocks priority move against grounded opponent`() {
		val fixture = publicBattleRuleFixture(
			name = "psychic-terrain-blocks-priority-move-against-grounded-opponent",
			inputSummary = "精神场地存在时，使用者对接地对手使用先制攻击。",
			expectedSummary = "技能消耗 PP 后被场地阻挡，不造成伤害。",
		)
		val prioritySkill = damagingSkill(name = "先制测试", priority = 1)
		val state = engine.start(
			initialState(
				first = participant("priority-user", speed = 50, skill = prioritySkill),
				second = participant("grounded-target", speed = 100),
				environment = BattleEnvironment(terrain = BattleTerrain.PSYCHIC),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("priority-user", skillId = 1, targetActorId = "grounded-target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("psychic-terrain-blocks-priority-move-against-grounded-opponent")
		assertEquals(34, resolved.participant("priority-user")?.skillSlot(1)?.remainingPp)
		assertEquals(100, resolved.participant("grounded-target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillBlockedByTerrain>().single()
		assertEquals(BattleTerrain.PSYCHIC, blocked.terrain)
		assertEquals("grounded-target", blocked.targetActorId)
	}

	@Test
	fun `psychic terrain does not block priority move against ungrounded opponent`() {
		val fixture = publicBattleRuleFixture(
			name = "psychic-terrain-does-not-block-priority-move-against-ungrounded-opponent",
			inputSummary = "精神场地存在时，使用者对非接地对手使用先制攻击。",
			expectedSummary = "场地不阻挡该目标，技能按普通伤害流程结算。",
		)
		val prioritySkill = damagingSkill(name = "先制测试", priority = 1)
		val state = engine.start(
			initialState(
				first = participant("priority-user", speed = 50, skill = prioritySkill),
				second = participant("ungrounded-target", speed = 100, grounded = false),
				environment = BattleEnvironment(terrain = BattleTerrain.PSYCHIC),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("priority-user", skillId = 1, targetActorId = "ungrounded-target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("psychic-terrain-does-not-block-priority-move-against-ungrounded-opponent")
		assertEquals(34, resolved.participant("priority-user")?.skillSlot(1)?.remainingPp)
		assertEquals(72, resolved.participant("ungrounded-target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByTerrain>())
		assertEquals(28, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	@Test
	fun `psychic terrain does not block priority move against ally`() {
		val fixture = publicBattleRuleFixture(
			name = "psychic-terrain-does-not-block-priority-move-against-ally",
			inputSummary = "双打中精神场地存在时，使用者对同侧接地成员使用先制攻击。",
			expectedSummary = "精神场地只阻止对手的先制技能，不阻止同侧目标；技能按普通伤害流程结算。",
		)
		val prioritySkill = damagingSkill(name = "同侧先制测试", priority = 1)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("priority-user", speed = 50, skill = prioritySkill),
				firstB = participant("grounded-ally", speed = 100),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70),
				environment = BattleEnvironment(terrain = BattleTerrain.PSYCHIC),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("priority-user", skillId = 1, targetActorId = "grounded-ally")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("psychic-terrain-does-not-block-priority-move-against-ally")
		assertEquals(34, resolved.participant("priority-user")?.skillSlot(1)?.remainingPp)
		assertEquals(72, resolved.participant("grounded-ally")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByTerrain>())
		assertEquals("grounded-ally", resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().targetActorId)
	}
}
