package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证稳定指定属性伤害提升携带道具的现代规则。
 *
 * 场景类型：道具 before-damage fixture。
 * 参考来源类型：公开成熟模拟器道具资料和公开规则说明。现代规则中，传统属性强化道具只在使用者技能属性匹配
 * 时把技能有效威力乘以 1.2；它们不会消费自身，不会造成反伤，也不会改变技能属性或成员属性。测试刻意避开属性一致
 * 加成，让断言只覆盖道具倍率本身。
 */
class BattleElementDamageBoostItemTests {
	private val engine = BattleEngine()

	@Test
	fun `element damage boost item multiplies matching element damage without recoil or consumption`() {
		val fixture = publicBattleRuleFixture(
			name = "element-damage-boost-item-multiplies-matching-damage",
			inputSummary = "使用者携带火属性伤害提升 1.2 倍的非消耗道具，使用非本系火属性物理技能攻击中性目标。",
			expectedSummary = "技能有效威力从 40 提升到 48，普通伤害从 19 提升到 23；道具不消费，也不产生反伤事件。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					elementId = 2,
					skill = damagingSkill(elementId = 10),
					itemId = 226,
					itemEffects = listOf(BattleItemEffect.ElementDamageBoost(elementId = 10, multiplier = 1.2)),
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

		fixture.assertNamed("element-damage-boost-item-multiplies-matching-damage")
		assertEquals(23, damage.amount)
		assertEquals(77, resolved.participant("defender")?.currentHp)
		assertEquals(100, attacker.currentHp)
		assertEquals(226, attacker.itemId)
		assertEquals(1, attacker.itemEffects.size)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.RecoilDamageApplied>())
	}

	@Test
	fun `element damage boost item keeps neutral multiplier for non matching element`() {
		val fixture = publicBattleRuleFixture(
			name = "element-damage-boost-item-ignores-non-matching-damage",
			inputSummary = "使用者携带水属性伤害提升 1.2 倍的非消耗道具，使用非本系火属性物理技能攻击中性目标。",
			expectedSummary = "技能属性和道具声明属性不匹配，因此普通伤害仍为 19，道具保持携带状态。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					elementId = 2,
					skill = damagingSkill(elementId = 10),
					itemId = 220,
					itemEffects = listOf(BattleItemEffect.ElementDamageBoost(elementId = 11, multiplier = 1.2)),
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

		fixture.assertNamed("element-damage-boost-item-ignores-non-matching-damage")
		assertEquals(19, damage.amount)
		assertEquals(81, resolved.participant("defender")?.currentHp)
		assertEquals(220, attacker.itemId)
	}
}
