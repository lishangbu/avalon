package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证按本次技能实际伤害量回复 HP 的非消耗携带道具。
 *
 * 场景类型：道具造成伤害后 fixture。
 * 参考来源类型：公开成熟模拟器道具资料。贝壳之铃类道具在一次技能动作结束后读取本次技能造成的总实际 HP
 * 损失，回复 `floor(totalDamage / 8)` 且最少 1 点；多段技能使用总伤害，而不是每段伤害分别触发回复。打到
 * 替身时，本体没有受伤，但替身实际损失的 HP 仍计入本次造成伤害。
 */
class BattleDamageDealtHealingItemTests {
	private val engine = BattleEngine()

	@Test
	fun `damage dealt healing item restores one eighth of body damage`() {
		val fixture = publicBattleRuleFixture(
			name = "damage-dealt-healing-item-restores-eighth-actual-damage",
			inputSummary = "使用者当前 HP 50，携带按造成伤害八分之一回复的道具，使用非本系 40 威力物理技能命中本体。",
			expectedSummary = "技能实际造成 19 点伤害；整次技能结束后使用者回复 floor(19 / 8) = 2 点 HP，道具不消费。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					currentHp = 50,
					elementId = 2,
					itemId = 230,
					itemEffects = listOf(BattleItemEffect.DamageDealtHeal(healDenominator = 8)),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val healing = resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single()
		val attacker = requireNotNull(resolved.participant("attacker"))

		fixture.assertNamed("damage-dealt-healing-item-restores-eighth-actual-damage")
		assertEquals(19, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(81, resolved.participant("defender")?.currentHp)
		assertEquals(52, attacker.currentHp)
		assertEquals(230, attacker.itemId)
		assertEquals(2, healing.amount)
	}

	@Test
	fun `damage dealt healing item counts substitute hp loss as actual damage`() {
		val fixture = publicBattleRuleFixture(
			name = "damage-dealt-healing-item-counts-substitute-damage",
			inputSummary = "使用者当前 HP 50，目标已有 25 HP 替身；使用者携带按造成伤害八分之一回复的道具攻击替身。",
			expectedSummary = "技能对替身实际造成 19 点伤害，目标本体 HP 不变；整次技能结束后使用者回复 2 点 HP。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					currentHp = 50,
					elementId = 2,
					itemId = 230,
					itemEffects = listOf(BattleItemEffect.DamageDealtHeal(healDenominator = 8)),
				),
				second = participant("defender", speed = 50).copy(substituteHp = 25),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val healing = resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single()
		val substituteDamage = resolved.events.filterIsInstance<BattleEvent.SubstituteDamageApplied>().single()
		val attacker = requireNotNull(resolved.participant("attacker"))
		val defender = requireNotNull(resolved.participant("defender"))

		fixture.assertNamed("damage-dealt-healing-item-counts-substitute-damage")
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(19, substituteDamage.amount)
		assertEquals(6, defender.substituteHp)
		assertEquals(100, defender.currentHp)
		assertEquals(52, attacker.currentHp)
		assertEquals(230, attacker.itemId)
		assertEquals(2, healing.amount)
	}

	@Test
	fun `damage dealt healing item uses total damage from multi hit skill once`() {
		val fixture = publicBattleRuleFixture(
			name = "damage-dealt-healing-item-uses-total-multi-hit-damage",
			inputSummary = "使用者当前 HP 50，携带按造成伤害八分之一回复的道具，使用固定三段的非本系物理技能。",
			expectedSummary = "三段技能各造成 19 点伤害，总伤害 57；整次技能结束后只触发一次回复，回复 floor(57 / 8) = 7 点 HP。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					currentHp = 50,
					elementId = 2,
					skill = damagingSkill(name = "三段测试", minHits = 3, maxHits = 3),
					itemId = 230,
					itemEffects = listOf(BattleItemEffect.DamageDealtHeal(healDenominator = 8)),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15, 1, 15, 1, 15)),
		)
		val healing = resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single()
		val damageAmounts = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().map { it.amount }

		fixture.assertNamed("damage-dealt-healing-item-uses-total-multi-hit-damage")
		assertEquals(listOf(19, 19, 19), damageAmounts)
		assertEquals(43, resolved.participant("defender")?.currentHp)
		assertEquals(57, resolved.participant("attacker")?.currentHp)
		assertEquals(7, healing.amount)
	}
}
