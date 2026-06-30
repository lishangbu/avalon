package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 验证一次性指定属性伤害减免携带道具的现代规则。
 *
 * 场景类型：道具 before-damage fixture。
 * 参考来源类型：公开成熟模拟器道具资料和公开规则说明。现代抗性树果在本体受到对应属性且效果绝佳的技能伤害时
 * 消费并把最终伤害乘以 0.5；一般属性抗性树果没有效果绝佳条件，只要求一般属性伤害命中本体。公开实现还要求
 * 命中替身时不触发抗性树果，所以这里把替身场景作为独立回归用例固定下来。
 */
class BattleElementDamageReductionItemTests {
	private val engine = BattleEngine()

	@Test
	fun `element damage reduction item halves super effective matching damage and consumes item`() {
		val fixture = publicBattleRuleFixture(
			name = "element-damage-reduction-item-halves-super-effective-damage",
			inputSummary = "目标携带火属性抗性树果，受到效果绝佳的非本系火属性物理技能命中。",
			expectedSummary = "普通伤害先因效果绝佳变为 38，再被道具按 0.5 倍削弱为 19；道具触发后被消费。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, elementId = 2, skill = damagingSkill(elementId = 10)),
				second = participant(
					"holder",
					speed = 50,
					elementId = 12,
					itemId = 161,
					itemEffects = listOf(BattleItemEffect.ElementDamageReduction(elementId = 10, multiplier = 0.5)),
				),
				rules = fireSuperEffectiveAgainstGrassRules(),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val holder = requireNotNull(resolved.participant("holder"))
		val reduced = resolved.events.filterIsInstance<BattleEvent.DamageReducedByItem>().single()
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		fixture.assertNamed("element-damage-reduction-item-halves-super-effective-damage")
		assertEquals(19, damage.amount)
		assertEquals(81, holder.currentHp)
		assertNull(holder.itemId)
		assertEquals(emptyList(), holder.itemEffects)
		assertEquals(161, reduced.itemId)
		assertEquals(10, reduced.elementId)
		assertEquals(0.5, reduced.multiplier)
		assertEquals(true, reduced.consumed)
	}

	/**
	 * 固定“抗性减伤先于伤害后回复”的结算顺序。
	 *
	 * 目标侧的抗性道具会在 HP 写入之前把普通伤害从 38 修正到 19；攻击方的按伤害回复道具则在整次技能结束后
	 * 读取实际造成伤害。这个用例把两个道具放在同一击里，避免后续重构把攻击方回复错误地接到减伤前的原始伤害，
	 * 也避免目标道具消费事件和攻击方回复事件因为状态快照覆盖而互相丢失。
	 */
	@Test
	fun `damage dealt healing item reads damage after element reduction item`() {
		val fixture = publicBattleRuleFixture(
			name = "damage-dealt-healing-item-reads-damage-after-element-reduction",
			inputSummary = "攻击方当前 HP 50 且携带按造成伤害回复道具；目标携带火属性抗性树果，受到效果绝佳火属性技能命中。",
			expectedSummary = "火属性伤害先从 38 点减半为 19 点；攻击方随后按 19 点实际伤害回复 2 点，目标道具被消费。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					currentHp = 50,
					elementId = 2,
					skill = damagingSkill(elementId = 10),
					itemId = 230,
					itemEffects = listOf(BattleItemEffect.DamageDealtHeal(healDenominator = 8)),
				),
				second = participant(
					"holder",
					speed = 50,
					elementId = 12,
					itemId = 161,
					itemEffects = listOf(BattleItemEffect.ElementDamageReduction(elementId = 10, multiplier = 0.5)),
				),
				rules = fireSuperEffectiveAgainstGrassRules(),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		val healing = resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single()

		fixture.assertNamed("damage-dealt-healing-item-reads-damage-after-element-reduction")
		assertEquals(19, damage.amount)
		assertEquals(81, resolved.participant("holder")?.currentHp)
		assertNull(resolved.participant("holder")?.itemId)
		assertEquals(52, resolved.participant("attacker")?.currentHp)
		assertEquals(2, healing.amount)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.DamageReducedByItem>().size)
	}

	@Test
	fun `element damage reduction item ignores matching damage that is not super effective`() {
		val fixture = publicBattleRuleFixture(
			name = "element-damage-reduction-item-requires-super-effective-damage",
			inputSummary = "目标携带火属性抗性树果，受到中性火属性物理技能命中。",
			expectedSummary = "技能属性匹配但不是效果绝佳，抗性树果不触发，伤害保持 19，道具仍然保留。",
		)
		val reduction = BattleItemEffect.ElementDamageReduction(elementId = 10, multiplier = 0.5)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, elementId = 2, skill = damagingSkill(elementId = 10)),
				second = participant(
					"holder",
					speed = 50,
					itemId = 161,
					itemEffects = listOf(reduction),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val holder = requireNotNull(resolved.participant("holder"))

		fixture.assertNamed("element-damage-reduction-item-requires-super-effective-damage")
		assertEquals(81, holder.currentHp)
		assertEquals(161, holder.itemId)
		assertEquals(listOf(reduction), holder.itemEffects)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageReducedByItem>())
	}

	@Test
	fun `element damage reduction item does not activate when substitute takes the hit`() {
		val fixture = publicBattleRuleFixture(
			name = "element-damage-reduction-item-does-not-activate-through-substitute",
			inputSummary = "目标携带火属性抗性树果并已有替身，受到效果绝佳的火属性物理技能命中。",
			expectedSummary = "技能先命中替身，本体没有直接受伤，抗性树果不触发也不消费。",
		)
		val reduction = BattleItemEffect.ElementDamageReduction(elementId = 10, multiplier = 0.5)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, elementId = 2, skill = damagingSkill(elementId = 10)),
				second = participant(
					"holder",
					speed = 50,
					elementId = 12,
					itemId = 161,
					itemEffects = listOf(reduction),
				).copy(substituteHp = 25),
				rules = fireSuperEffectiveAgainstGrassRules(),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val holder = requireNotNull(resolved.participant("holder"))
		val substituteDamage = resolved.events.filterIsInstance<BattleEvent.SubstituteDamageApplied>().single()

		fixture.assertNamed("element-damage-reduction-item-does-not-activate-through-substitute")
		assertEquals(100, holder.currentHp)
		assertEquals(161, holder.itemId)
		assertEquals(listOf(reduction), holder.itemEffects)
		assertEquals(25, substituteDamage.amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageReducedByItem>())
	}

	@Test
	fun `normal damage reduction item halves normal damage without super effective condition`() {
		val fixture = publicBattleRuleFixture(
			name = "normal-damage-reduction-item-halves-normal-damage",
			inputSummary = "目标携带一般属性抗性树果，受到中性一般属性物理技能命中。",
			expectedSummary = "一般属性抗性树果不要求效果绝佳，直接把 19 点普通伤害削弱为 9 并消费道具。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, elementId = 2, skill = damagingSkill(elementId = 1)),
				second = participant(
					"holder",
					speed = 50,
					itemId = 177,
					itemEffects = listOf(
						BattleItemEffect.ElementDamageReduction(
							elementId = 1,
							multiplier = 0.5,
							requiresSuperEffective = false,
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val holder = requireNotNull(resolved.participant("holder"))
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		fixture.assertNamed("normal-damage-reduction-item-halves-normal-damage")
		assertEquals(9, damage.amount)
		assertEquals(91, holder.currentHp)
		assertNull(holder.itemId)
		assertEquals(emptyList(), holder.itemEffects)
	}

	private fun fireSuperEffectiveAgainstGrassRules() =
		neutralRules().copy(
			elementChart = ElementEffectivenessChart(
				mapOf(10L to mapOf(12L to 2.0)),
			),
		)
}
