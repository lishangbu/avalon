package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 携带道具公开对照 场景。
 *
 * 这组测试专门钉住道具触发时机和数值口径：伤害增幅道具由伤害计算器读取倍率，主动攻击后的反伤、接触反伤和
 * 回复则由状态机在正确时机修改 HP。每个场景都给出公开规则或成熟引擎来源，避免道具效果只靠本地直觉实现。
 */
class BattleHeldItemPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `contact damage item damages attacker by one sixth max hp after contact`() {
		val scenario = publicBattleRuleScenario(
			name = "contact-damage-item-damages-attacker-by-one-sixth-after-contact",
			inputSummary = "攻击方最大 HP 100，使用接触类物理技能命中目标；目标携带接触后按攻击方最大 HP 1/6 反伤的道具。",
			expectedSummary = "目标先受到普通伤害；随后攻击方受到 floor(100 / 6) = 16 点反伤。该反伤不消费目标道具。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = damagingSkill(makesContact = true),
				),
				second = participant(
					"defender",
					speed = 50,
					itemId = 583,
					itemEffects = listOf(BattleItemEffect.ContactDamageToAttacker(damageDenominator = 6)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val recoil = resolved.events.filterIsInstance<BattleEvent.RecoilDamageApplied>().single()
		val defender = requireNotNull(resolved.participant("defender"))

		scenario.assertNamed("contact-damage-item-damages-attacker-by-one-sixth-after-contact")
		assertEquals(72, defender.currentHp)
		assertEquals(583, defender.itemId)
		assertEquals(84, resolved.participant("attacker")?.currentHp)
		assertEquals(16, recoil.amount)
	}

	@Test
	fun `contact damage item is blocked by attacker contact side effect immunity`() {
		val scenario = publicBattleRuleScenario(
			name = "contact-damage-item-blocked-by-contact-side-effect-immunity",
			inputSummary = "攻击方使用接触类物理技能命中目标，目标携带接触反伤道具；攻击方携带免疫接触副作用的结构化道具效果。",
			expectedSummary = "目标正常受到伤害；攻击方不会受到接触反伤，也不会追加反伤事件。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = damagingSkill(makesContact = true),
					itemEffects = listOf(BattleItemEffect.ContactSideEffectImmunity),
				),
				second = participant(
					"defender",
					speed = 50,
					itemEffects = listOf(BattleItemEffect.ContactDamageToAttacker(damageDenominator = 6)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("contact-damage-item-blocked-by-contact-side-effect-immunity")
		assertEquals(72, resolved.participant("defender")?.currentHp)
		assertEquals(100, resolved.participant("attacker")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.RecoilDamageApplied>())
	}

	@Test
	fun `contact damage item is blocked when punch based contact is suppressed`() {
		val scenario = publicBattleRuleScenario(
			name = "contact-damage-item-blocked-when-punch-based-contact-is-suppressed",
			inputSummary = "攻击方使用原本接触的拳击类物理技能命中目标，目标携带接触反伤道具；攻击方携带让拳击类技能本次不接触的结构化道具效果。",
			expectedSummary = "目标正常受到伤害；本次技能已经不构成接触，因此攻击方不会受到接触反伤。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = damagingSkill(makesContact = true, punchBased = true),
					itemEffects = listOf(BattleItemEffect.PunchBasedContactSuppression),
				),
				second = participant(
					"defender",
					speed = 50,
					itemEffects = listOf(BattleItemEffect.ContactDamageToAttacker(damageDenominator = 6)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("contact-damage-item-blocked-when-punch-based-contact-is-suppressed")
		assertEquals(72, resolved.participant("defender")?.currentHp)
		assertEquals(100, resolved.participant("attacker")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.RecoilDamageApplied>())
	}

	@Test
	fun `damage boost item uses max hp recoil like public item scenario`() {
		val scenario = publicBattleRuleScenario(
			name = "damage-boost-item-uses-max-hp-recoil-after-damage",
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

		scenario.assertNamed("damage-boost-item-uses-max-hp-recoil-after-damage")
		assertEquals(63, resolved.participant("defender")?.currentHp)
		assertEquals(90, resolved.participant("attacker")?.currentHp)
		assertEquals(10, recoil.amount)
	}

	@Test
	fun `end turn healing item restores one sixteenth max hp like public item scenario`() {
		val scenario = publicBattleRuleScenario(
			name = "end-turn-healing-item-restores-one-sixteenth-max-hp",
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

		scenario.assertNamed("end-turn-healing-item-restores-one-sixteenth-max-hp")
		assertEquals(86, resolved.participant("holder")?.currentHp)
		assertEquals(6, healing.amount)
	}
}
