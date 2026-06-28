package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFatalDamageSurvivalSource
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 验证满 HP 致命伤害保留 1 HP 的现代规则。
 *
 * 场景类型：伤害写入前的保命特性和一次性保命携带道具 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。该规则只在技能直接伤害会从满 HP 打倒目标时触发；
 * 特性来源不消耗，道具来源触发后移除携带道具。非满 HP、间接伤害和后续绕过特性的规则将由各自 fixture 扩展。
 */
class BattleFatalDamageSurvivalTests {
	private val engine = BattleEngine()

	@Test
	fun `full hp survival ability leaves target at one hp before faint`() {
		val fixture = publicBattleRuleFixture(
			name = "full-hp-survival-ability-leaves-one-hp-before-faint",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Sturdy_(Ability)",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
			),
			inputSummary = "目标满 HP 且拥有满 HP 致命伤害保留 1 HP 的结构化特性效果，受到足以一击倒下的普通技能伤害。",
			expectedSummary = "目标没有倒下，最终保留 1 HP；事件流记录本次致命伤害由特性来源保住。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = lethalSkill()),
				second = participant(
					"survivor",
					speed = 50,
					abilityId = 5,
					abilityEffects = listOf(BattleAbilityEffect.SurviveFatalDamageAtFullHp()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 9001, targetActorId = "survivor")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val survived = resolved.events.filterIsInstance<BattleEvent.FatalDamageSurvived>().single()
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		fixture.assertNamed("full-hp-survival-ability-leaves-one-hp-before-faint")
		assertEquals(1, resolved.participant("survivor")?.currentHp)
		assertNull(resolved.result)
		assertEquals(99, damage.amount)
		assertEquals(BattleFatalDamageSurvivalSource.ABILITY, survived.source)
		assertEquals(5, survived.sourceId)
		assertEquals(false, survived.consumed)
		assertEquals(survived.incomingDamage - damage.amount, survived.preventedDamage)
	}

	@Test
	fun `consumable full hp survival item leaves target at one hp and consumes item`() {
		val fixture = publicBattleRuleFixture(
			name = "consumable-full-hp-survival-item-leaves-one-hp-and-consumes-item",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Focus_Sash",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
			),
			inputSummary = "目标满 HP 且携带一次性满 HP 致命伤害保留 1 HP 道具，受到足以一击倒下的普通技能伤害。",
			expectedSummary = "目标最终保留 1 HP，道具被消费，事件流记录本次保命来源是携带道具。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = lethalSkill()),
				second = participant(
					"survivor",
					speed = 50,
					itemId = 252,
					itemEffects = listOf(BattleItemEffect.SurviveFatalDamageAtFullHp()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 9001, targetActorId = "survivor")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val survivor = resolved.participant("survivor")
		val survived = resolved.events.filterIsInstance<BattleEvent.FatalDamageSurvived>().single()

		fixture.assertNamed("consumable-full-hp-survival-item-leaves-one-hp-and-consumes-item")
		assertEquals(1, survivor?.currentHp)
		assertNull(survivor?.itemId)
		assertEquals(emptyList(), survivor?.itemEffects)
		assertEquals(BattleFatalDamageSurvivalSource.ITEM, survived.source)
		assertEquals(252, survived.sourceId)
		assertEquals(true, survived.consumed)
	}

	@Test
	fun `full hp survival does not trigger after prior damage`() {
		val fixture = publicBattleRuleFixture(
			name = "full-hp-survival-does-not-trigger-after-prior-damage",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Sturdy_(Ability)",
				"https://bulbapedia.bulbagarden.net/wiki/Focus_Sash",
			),
			inputSummary = "目标已经不是满 HP，拥有满 HP 致命伤害保留 1 HP 效果，随后受到足以倒下的普通技能伤害。",
			expectedSummary = "保命效果不触发，目标 HP 被扣到 0，事件流不出现保留 1 HP 事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = lethalSkill()),
				second = participant(
					"target",
					speed = 50,
					currentHp = 99,
					abilityId = 5,
					abilityEffects = listOf(BattleAbilityEffect.SurviveFatalDamageAtFullHp()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 9001, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("full-hp-survival-does-not-trigger-after-prior-damage")
		assertEquals(0, resolved.participant("target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.FatalDamageSurvived>())
		assertEquals(99, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	private fun lethalSkill() =
		damagingSkill(
			skillId = 9001,
			name = "致命伤害测试",
			power = 250,
		)
}
