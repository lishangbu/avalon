package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证按伤害分类或效果绝佳条件提升伤害的非消耗携带道具。
 *
 * 场景类型：道具伤害公式 fixture。
 * 参考来源类型：公开成熟模拟器道具资料。力量头带和博识眼镜属于威力阶段修正，会先把对应分类技能的有效威力
 * 提高 10% 再进入普通伤害公式；达人带属于最终伤害阶段修正，只在本次技能对目标效果绝佳时把最终伤害提高 20%。
 * 这些道具都不会消费自身，也不会附带反伤、锁招或其它道具生命周期变化。
 */
class BattleConditionalDamageBoostItemTests {
	private val engine = BattleEngine()

	@Test
	fun `damage class power boost item raises matching physical power`() {
		val fixture = publicBattleRuleFixture(
			name = "physical-power-boost-item-raises-matching-damage-class",
			sourceUrls = listOf("https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts"),
			inputSummary = "使用者携带物理威力提升道具，使用非本系 40 威力物理技能攻击中性目标。",
			expectedSummary = "技能有效威力从 40 提升到 44，普通伤害从 19 提升到 21，道具保持携带状态。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					elementId = 2,
					itemId = 243,
					itemEffects = listOf(
						BattleItemEffect.DamageClassPowerBoost(
							damageClasses = setOf(BattleDamageClass.PHYSICAL),
							multiplier = 1.1,
						),
					),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		val attacker = requireNotNull(resolved.participant("attacker"))

		fixture.assertNamed("physical-power-boost-item-raises-matching-damage-class")
		assertEquals(21, damage.amount)
		assertEquals(79, resolved.participant("defender")?.currentHp)
		assertEquals(243, attacker.itemId)
	}

	@Test
	fun `damage class power boost item ignores non matching damage class`() {
		val fixture = publicBattleRuleFixture(
			name = "physical-power-boost-item-ignores-special-damage-class",
			sourceUrls = listOf("https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts"),
			inputSummary = "使用者携带物理威力提升道具，使用非本系 40 威力特殊技能攻击中性目标。",
			expectedSummary = "技能伤害分类不匹配，道具不提供威力修正，普通伤害仍为 19。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					elementId = 2,
					skill = damagingSkill(damageClass = BattleDamageClass.SPECIAL),
					itemId = 243,
					itemEffects = listOf(
						BattleItemEffect.DamageClassPowerBoost(
							damageClasses = setOf(BattleDamageClass.PHYSICAL),
							multiplier = 1.1,
						),
					),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		fixture.assertNamed("physical-power-boost-item-ignores-special-damage-class")
		assertEquals(19, damage.amount)
		assertEquals(81, resolved.participant("defender")?.currentHp)
	}

	@Test
	fun `super effective damage boost item raises final super effective damage`() {
		val fixture = publicBattleRuleFixture(
			name = "super-effective-damage-boost-item-raises-final-damage",
			sourceUrls = listOf("https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts"),
			inputSummary = "使用者携带效果绝佳伤害提升道具，使用非本系火属性物理技能攻击草属性目标。",
			expectedSummary = "普通伤害先因效果绝佳从 19 变为 38，再由道具按最终 1.2 倍修正为 45。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					elementId = 2,
					skill = damagingSkill(elementId = 10),
					itemId = 245,
					itemEffects = listOf(BattleItemEffect.SuperEffectiveDamageBoost(multiplier = 1.2)),
				),
				second = participant("defender", speed = 50, elementId = 12),
				rules = fireSuperEffectiveAgainstGrassRules(),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		val attacker = requireNotNull(resolved.participant("attacker"))

		fixture.assertNamed("super-effective-damage-boost-item-raises-final-damage")
		assertEquals(45, damage.amount)
		assertEquals(55, resolved.participant("defender")?.currentHp)
		assertEquals(245, attacker.itemId)
	}

	@Test
	fun `super effective damage boost item ignores neutral damage`() {
		val fixture = publicBattleRuleFixture(
			name = "super-effective-damage-boost-item-ignores-neutral-damage",
			sourceUrls = listOf("https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts"),
			inputSummary = "使用者携带效果绝佳伤害提升道具，使用非本系火属性物理技能攻击中性目标。",
			expectedSummary = "属性克制结果不是效果绝佳，道具不提供最终伤害修正，普通伤害仍为 19。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					elementId = 2,
					skill = damagingSkill(elementId = 10),
					itemId = 245,
					itemEffects = listOf(BattleItemEffect.SuperEffectiveDamageBoost(multiplier = 1.2)),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		fixture.assertNamed("super-effective-damage-boost-item-ignores-neutral-damage")
		assertEquals(19, damage.amount)
		assertEquals(81, resolved.participant("defender")?.currentHp)
	}

	private fun fireSuperEffectiveAgainstGrassRules() =
		neutralRules().copy(
			elementChart = ElementEffectivenessChart(
				mapOf(10L to mapOf(12L to 2.0)),
			),
		)
}
