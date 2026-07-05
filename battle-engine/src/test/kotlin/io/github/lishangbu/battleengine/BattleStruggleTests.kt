package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证无可选技能时的内置挣扎规则。
 *
 * 场景类型：行动 fallback 与现代无属性伤害场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则中，当成员没有任何可正常选择的技能时，回合行动会
 * 自动改用挣扎；挣扎是 50 威力物理无属性伤害，随机相邻对手为目标，不消耗原技能 PP，成功造成伤害后使用者按
 * 自身最大 HP 的 1/4 承受自损，且该自损不被普通反作用伤害免疫或泛用间接伤害免疫阻止。
 * 验证重点：引擎不能把挣扎建成数据库技能，也不能让原技能的属性相性、属性型道具或原技能 PP 污染本次结算。
 */
class BattleStruggleTests {
	private val engine = BattleEngine()

	@Test
	fun `all exhausted skills automatically use typeless struggle without consuming original pp`() {
		val scenario = publicBattleRuleScenario(
			name = "all-exhausted-skills-use-typeless-struggle",
			inputSummary = "使用者所有技能 PP 都耗尽，提交原技能攻击一个对原技能属性免疫的目标。",
			expectedSummary = "回合改用挣扎；挣扎按无属性伤害命中目标，原技能 PP 保持 0，使用者按最大 HP 的 1/4 自损。",
		)
		val exhaustedSkill = damagingSkill(skillId = 10, name = "原技能", elementId = 1)
			.copy(remainingPp = 0, maxPp = 35)
		val state = engine.start(
			initialState(
				first = participant("struggler", speed = 100, currentHp = 100, skill = exhaustedSkill),
				second = participant("immune-target", speed = 50, elementId = 2),
				rules = neutralRules().copy(
					elementChart = ElementEffectivenessChart(mapOf(1L to mapOf(2L to 0.0))),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("struggler", skillId = 10, targetActorId = "immune-target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("all-exhausted-skills-use-typeless-struggle")
		val skillUsed = resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single()
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		val recoil = resolved.events.filterIsInstance<BattleEvent.SkillRecoilDamageApplied>().single()
		assertEquals(BattleTurnActionPlanner.STRUGGLE_SKILL_ID, skillUsed.skillId)
		assertEquals("挣扎", skillUsed.skillName)
		assertEquals(BattleTurnActionPlanner.STRUGGLE_SKILL_ID, damage.skillId)
		assertTrue(damage.amount > 0)
		assertEquals(1.0, damage.effectiveness)
		assertEquals(25, recoil.amount)
		assertEquals(100, recoil.sourceDamageAmount)
		assertEquals(75, resolved.participant("struggler")?.currentHp)
		assertEquals(0, resolved.participant("struggler")?.skillSlot(10)?.remainingPp)
	}

	@Test
	fun `struggle max hp recoil is not prevented by recoil or indirect damage immunity`() {
		val scenario = publicBattleRuleScenario(
			name = "struggle-recoil-bypasses-recoil-and-indirect-damage-immunity",
			inputSummary = "使用者带有反作用伤害免疫和间接伤害免疫效果，所有原技能 PP 耗尽后发动挣扎。",
			expectedSummary = "挣扎成功造成伤害后仍按使用者最大 HP 的 1/4 自损，说明该代价没有被普通自损免疫误拦截。",
		)
		val exhaustedSkill = damagingSkill(skillId = 10, name = "原技能").copy(remainingPp = 0, maxPp = 35)
		val state = engine.start(
			initialState(
				first = participant(
					"immune-struggler",
					speed = 100,
					currentHp = 100,
					skill = exhaustedSkill,
					abilityEffects = listOf(
						BattleAbilityEffect.SkillRecoilDamageImmunity,
						BattleAbilityEffect.IndirectDamageImmunity,
					),
				),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("immune-struggler", skillId = 10, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("struggle-recoil-bypasses-recoil-and-indirect-damage-immunity")
		val recoil = resolved.events.filterIsInstance<BattleEvent.SkillRecoilDamageApplied>().single()
		assertEquals(BattleTurnActionPlanner.STRUGGLE_SKILL_ID, recoil.skillId)
		assertEquals(25, recoil.amount)
		assertEquals(75, resolved.participant("immune-struggler")?.currentHp)
	}
}
