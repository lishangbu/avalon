package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 携带道具公开对照 fixture。
 *
 * 这组测试专门钉住道具触发时机和数值口径：伤害增幅道具由伤害计算器读取倍率，反伤和回复则由状态机在正确
 * 时机修改 HP。每个场景都给出公开规则或成熟引擎来源，避免道具效果只靠本地直觉实现。
 */
class BattleHeldItemPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `damage boost item uses max hp recoil like public item fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "damage-boost-item-uses-max-hp-recoil-after-damage",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Life_Orb",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
			),
			inputSummary = "使用者最大 HP 100，携带造成伤害提升 1.3 倍且反伤分母为 10 的道具，使用普通物理技能造成伤害。",
			expectedSummary = "目标受到 1.3 倍后的伤害；使用者按最大 HP 的 1/10 失去 10 HP，而不是按造成伤害的 1/10 失去 HP。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					itemEffects = listOf(BattleItemEffect.DamageBoostWithRecoil(multiplier = 1.3, recoilDenominator = 10)),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val recoil = resolved.events.filterIsInstance<BattleEvent.RecoilDamageApplied>().single()

		fixture.assertNamed("damage-boost-item-uses-max-hp-recoil-after-damage")
		assertEquals(63, resolved.participant("defender")?.currentHp)
		assertEquals(90, resolved.participant("attacker")?.currentHp)
		assertEquals(10, recoil.amount)
	}

	@Test
	fun `end turn healing item restores one sixteenth max hp like public item fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "end-turn-healing-item-restores-one-sixteenth-max-hp",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Leftovers",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
			),
			inputSummary = "持有者最大 HP 100，当前 HP 80，携带回合末回复分母为 16 的道具，本回合没有其它行动。",
			expectedSummary = "完整回合末回复 floor(100 / 16) = 6 点 HP，最终 HP 为 86，并产生一次回复事件。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"holder",
					speed = 100,
					currentHp = 80,
					itemEffects = listOf(BattleItemEffect.HeldEndTurnHeal(healDenominator = 16)),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))
		val healing = resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single()

		fixture.assertNamed("end-turn-healing-item-restores-one-sixteenth-max-hp")
		assertEquals(86, resolved.participant("holder")?.currentHp)
		assertEquals(6, healing.amount)
	}
}
