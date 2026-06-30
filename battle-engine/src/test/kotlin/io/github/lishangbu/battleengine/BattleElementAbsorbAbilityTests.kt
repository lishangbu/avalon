package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证目标特性吸收指定属性技能并转换为回复。
 *
 * 场景类型：目标前置条件 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则中，部分特性会让目标在被指定属性技能命中后不再
 * 承受该技能的普通效果，并按最大 HP 的固定比例回复；目标满 HP 时仍会触发免疫，但实际回复量为 0。
 */
class BattleElementAbsorbAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `element absorb ability heals and blocks matching damage`() {
		val scenario = publicBattleRuleScenario(
			name = "element-absorb-ability-heals-and-blocks-matching-damage",
			inputSummary = "目标拥有吸收指定属性技能并回复的结构化特性，当前 HP 不满，对手使用匹配属性攻击。",
			expectedSummary = "技能命中后被目标特性吸收，目标不受伤害，并按最大 HP 的 1/4 回复。",
		)
		val skill = damagingSkill(name = "吸收伤害测试", elementId = 13)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = skill),
				second = participant(
					"absorber",
					speed = 80,
					currentHp = 50,
					abilityId = 10,
					abilityEffects = listOf(BattleAbilityEffect.ElementSkillAbsorbHeal(elementId = 13)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "absorber")),
			ScriptedBattleRandom(emptyList()),
		)
		val absorbed = resolved.events.filterIsInstance<BattleEvent.SkillAbsorbedByAbility>().single()

		scenario.assertNamed("element-absorb-ability-heals-and-blocks-matching-damage")
		assertEquals(34, resolved.participant("attacker")?.skillSlot(1)?.remainingPp)
		assertEquals(75, resolved.participant("absorber")?.currentHp)
		assertEquals(25, absorbed.healAmount)
		assertEquals(13, absorbed.elementId)
		assertEquals(10, absorbed.abilityId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
	}

	@Test
	fun `element absorb ability at full hp blocks without overhealing`() {
		val scenario = publicBattleRuleScenario(
			name = "element-absorb-ability-at-full-hp-blocks-without-overhealing",
			inputSummary = "目标拥有吸收指定属性技能并回复的结构化特性，但当前已经满 HP，对手使用匹配属性攻击。",
			expectedSummary = "技能仍被目标特性吸收并阻止伤害，实际回复量为 0，目标 HP 不超过最大值。",
		)
		val skill = damagingSkill(name = "满血吸收测试", elementId = 11)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = skill),
				second = participant(
					"absorber",
					speed = 80,
					abilityId = 11,
					abilityEffects = listOf(BattleAbilityEffect.ElementSkillAbsorbHeal(elementId = 11)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "absorber")),
			ScriptedBattleRandom(emptyList()),
		)
		val absorbed = resolved.events.filterIsInstance<BattleEvent.SkillAbsorbedByAbility>().single()

		scenario.assertNamed("element-absorb-ability-at-full-hp-blocks-without-overhealing")
		assertEquals(100, resolved.participant("absorber")?.currentHp)
		assertEquals(0, absorbed.healAmount)
		assertEquals(11, absorbed.elementId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
	}

	@Test
	fun `element absorb ability blocks matching status skill`() {
		val scenario = publicBattleRuleScenario(
			name = "element-absorb-ability-blocks-matching-status-skill",
			inputSummary = "目标拥有吸收指定属性技能并回复的结构化特性，对手使用匹配属性变化技能并试图降低目标能力阶级。",
			expectedSummary = "变化技能命中后被目标特性吸收，不写入能力阶级变化，目标按最大 HP 的 1/4 回复。",
		)
		val skill = damagingSkill(
			name = "吸收变化测试",
			elementId = 5,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.ACCURACY,
					target = BattleEffectTarget.TARGET,
					stageDelta = -1,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = skill),
				second = participant(
					"absorber",
					speed = 80,
					currentHp = 70,
					abilityId = 297,
					abilityEffects = listOf(BattleAbilityEffect.ElementSkillAbsorbHeal(elementId = 5)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "absorber")),
			ScriptedBattleRandom(emptyList()),
		)
		val absorbed = resolved.events.filterIsInstance<BattleEvent.SkillAbsorbedByAbility>().single()

		scenario.assertNamed("element-absorb-ability-blocks-matching-status-skill")
		assertEquals(95, resolved.participant("absorber")?.currentHp)
		assertEquals(25, absorbed.healAmount)
		assertEquals(5, absorbed.elementId)
		assertEquals(0, resolved.participant("absorber")?.statStage(BattleStat.ACCURACY))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageChanged>())
	}
}
