package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证目标特性吸收指定属性技能并提升自身能力阶级。
 *
 * 场景类型：目标前置条件 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则中，部分特性会让目标在被指定属性技能命中后不再
 * 承受该技能的普通效果，并提升自身速度、攻击或防御等能力阶级。
 */
class BattleElementAbsorbStatAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `element absorb stat ability blocks electric damage and raises speed`() {
		val scenario = publicBattleRuleScenario(
			name = "element-absorb-stat-ability-blocks-electric-damage-and-raises-speed",
			inputSummary = "目标拥有吸收电属性技能并提升速度的结构化特性，对手使用电属性攻击。",
			expectedSummary = "技能命中后被目标特性吸收，目标不受伤害，速度能力阶级提升 1 级。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(name = "电吸收提速测试", elementId = 13)),
				second = participant(
					"absorber",
					speed = 80,
					abilityId = 78,
					abilityEffects = listOf(
						BattleAbilityEffect.ElementSkillAbsorbStatStage(
							elementId = 13,
							stat = BattleStat.SPEED,
							stageDelta = 1,
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "absorber")),
			ScriptedBattleRandom(emptyList()),
		)
		val absorbed = resolved.events.filterIsInstance<BattleEvent.SkillAbsorbedByAbility>().single()
		val statChanged = resolved.events.filterIsInstance<BattleEvent.StatStageChanged>().single()

		scenario.assertNamed("element-absorb-stat-ability-blocks-electric-damage-and-raises-speed")
		assertEquals(100, resolved.participant("absorber")?.currentHp)
		assertEquals(1, resolved.participant("absorber")?.statStage(BattleStat.SPEED))
		assertEquals(78, absorbed.abilityId)
		assertEquals(13, absorbed.elementId)
		assertEquals(0, absorbed.healAmount)
		assertEquals(BattleStat.SPEED, statChanged.stat)
		assertEquals(1, statChanged.delta)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
	}

	@Test
	fun `element absorb stat ability blocks grass damage and raises attack`() {
		val scenario = publicBattleRuleScenario(
			name = "element-absorb-stat-ability-blocks-grass-damage-and-raises-attack",
			inputSummary = "目标拥有吸收草属性技能并提升攻击的结构化特性，对手使用草属性攻击。",
			expectedSummary = "技能命中后被目标特性吸收，目标不受伤害，攻击能力阶级提升 1 级。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(name = "草吸收提攻测试", elementId = 12)),
				second = participant(
					"absorber",
					speed = 80,
					abilityId = 157,
					abilityEffects = listOf(
						BattleAbilityEffect.ElementSkillAbsorbStatStage(
							elementId = 12,
							stat = BattleStat.ATTACK,
							stageDelta = 1,
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "absorber")),
			ScriptedBattleRandom(emptyList()),
		)
		val statChanged = resolved.events.filterIsInstance<BattleEvent.StatStageChanged>().single()

		scenario.assertNamed("element-absorb-stat-ability-blocks-grass-damage-and-raises-attack")
		assertEquals(100, resolved.participant("absorber")?.currentHp)
		assertEquals(1, resolved.participant("absorber")?.statStage(BattleStat.ATTACK))
		assertEquals(BattleStat.ATTACK, statChanged.stat)
		assertEquals(1, statChanged.delta)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
	}

	@Test
	fun `element absorb stat ability blocks fire damage and raises defense by two`() {
		val scenario = publicBattleRuleScenario(
			name = "element-absorb-stat-ability-blocks-fire-damage-and-raises-defense-by-two",
			inputSummary = "目标拥有吸收火属性技能并大幅提升防御的结构化特性，对手使用火属性攻击。",
			expectedSummary = "技能命中后被目标特性吸收，目标不受伤害，防御能力阶级提升 2 级。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(name = "火吸收提防测试", elementId = 10)),
				second = participant(
					"absorber",
					speed = 80,
					abilityId = 273,
					abilityEffects = listOf(
						BattleAbilityEffect.ElementSkillAbsorbStatStage(
							elementId = 10,
							stat = BattleStat.DEFENSE,
							stageDelta = 2,
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "absorber")),
			ScriptedBattleRandom(emptyList()),
		)
		val statChanged = resolved.events.filterIsInstance<BattleEvent.StatStageChanged>().single()

		scenario.assertNamed("element-absorb-stat-ability-blocks-fire-damage-and-raises-defense-by-two")
		assertEquals(100, resolved.participant("absorber")?.currentHp)
		assertEquals(2, resolved.participant("absorber")?.statStage(BattleStat.DEFENSE))
		assertEquals(BattleStat.DEFENSE, statChanged.stat)
		assertEquals(2, statChanged.delta)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
	}
}
