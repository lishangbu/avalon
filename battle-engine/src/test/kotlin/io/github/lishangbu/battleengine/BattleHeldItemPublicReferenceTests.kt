package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
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

	@Test
	fun `end turn damage item hurts holder by one eighth max hp`() {
		val scenario = publicBattleRuleScenario(
			name = "end-turn-damage-item-hurts-holder-by-one-eighth-max-hp",
			inputSummary = "持有者最大 HP 100，当前 HP 100，携带会在回合末造成最大 HP 1/8 间接伤害的道具，本回合没有其它行动。",
			expectedSummary = "完整回合末持有者受到 floor(100 / 8) = 12 点道具伤害，最终 HP 为 88。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"holder",
					speed = 100,
					itemId = 265,
					itemEffects = listOf(BattleItemEffect.HeldEndTurnDamage(damageDenominator = 8)),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))
		val itemDamage = resolved.events.filterIsInstance<BattleEvent.HeldItemDamageApplied>().single()

		scenario.assertNamed("end-turn-damage-item-hurts-holder-by-one-eighth-max-hp")
		assertEquals(88, resolved.participant("holder")?.currentHp)
		assertEquals(265, itemDamage.itemId)
		assertEquals(12, itemDamage.amount)
	}

	@Test
	fun `end turn damage item is blocked by indirect damage immunity`() {
		val scenario = publicBattleRuleScenario(
			name = "end-turn-damage-item-blocked-by-indirect-damage-immunity",
			inputSummary = "持有者最大 HP 100，携带回合末自伤道具，同时拥有免疫间接伤害的结构化特性效果。",
			expectedSummary = "回合末道具自伤被间接伤害免疫阻止，HP 不变，也不会追加道具伤害事件。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"holder",
					speed = 100,
					abilityEffects = listOf(BattleAbilityEffect.IndirectDamageImmunity),
					itemId = 265,
					itemEffects = listOf(BattleItemEffect.HeldEndTurnDamage(damageDenominator = 8)),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		scenario.assertNamed("end-turn-damage-item-blocked-by-indirect-damage-immunity")
		assertEquals(100, resolved.participant("holder")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.HeldItemDamageApplied>())
	}

	@Test
	fun `contact transfer item moves to empty handed attacker and hurts new holder at end turn`() {
		val stickyBarbEffects = listOf(
			BattleItemEffect.ContactTransferToAttacker,
			BattleItemEffect.HeldEndTurnDamage(damageDenominator = 8),
		)
		val scenario = publicBattleRuleScenario(
			name = "contact-transfer-item-moves-to-empty-handed-attacker-and-hurts-new-holder",
			inputSummary = "目标携带接触转移且回合末自伤的道具；无道具攻击方用接触类物理技能命中目标本体。",
			expectedSummary = "伤害后道具从目标转移到攻击方；同一回合末新的持有者攻击方受到 floor(100 / 8) = 12 点道具伤害。",
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
					itemId = 265,
					itemEffects = stickyBarbEffects,
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val transfer = resolved.events.filterIsInstance<BattleEvent.HeldItemTransferred>().single()
		val itemDamage = resolved.events.filterIsInstance<BattleEvent.HeldItemDamageApplied>().single()
		val attacker = requireNotNull(resolved.participant("attacker"))
		val defender = requireNotNull(resolved.participant("defender"))

		scenario.assertNamed("contact-transfer-item-moves-to-empty-handed-attacker-and-hurts-new-holder")
		assertEquals(265, transfer.itemId)
		assertEquals("defender", transfer.fromActorId)
		assertEquals("attacker", transfer.toActorId)
		assertEquals(265, attacker.itemId)
		assertEquals(stickyBarbEffects, attacker.itemEffects)
		assertEquals(null, defender.itemId)
		assertEquals(emptyList(), defender.itemEffects)
		assertEquals(88, attacker.currentHp)
		assertEquals("attacker", itemDamage.actorId)
		assertEquals(12, itemDamage.amount)
	}

	@Test
	fun `contact transfer item stays on defender when attacker already has item`() {
		val stickyBarbEffects = listOf(
			BattleItemEffect.ContactTransferToAttacker,
			BattleItemEffect.HeldEndTurnDamage(damageDenominator = 8),
		)
		val scenario = publicBattleRuleScenario(
			name = "contact-transfer-item-stays-on-defender-when-attacker-already-has-item",
			inputSummary = "目标携带接触转移且回合末自伤的道具；攻击方已经持有其它道具并使用接触类物理技能命中目标。",
			expectedSummary = "攻击方已有携带道具，接触转移不发生；回合末仍由原持有者目标承受 1/8 最大 HP 的道具伤害。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					itemId = 999,
					skill = damagingSkill(makesContact = true),
				),
				second = participant(
					"defender",
					speed = 50,
					itemId = 265,
					itemEffects = stickyBarbEffects,
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val itemDamage = resolved.events.filterIsInstance<BattleEvent.HeldItemDamageApplied>().single()

		scenario.assertNamed("contact-transfer-item-stays-on-defender-when-attacker-already-has-item")
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.HeldItemTransferred>())
		assertEquals(999, resolved.participant("attacker")?.itemId)
		assertEquals(265, resolved.participant("defender")?.itemId)
		assertEquals("defender", itemDamage.actorId)
		assertEquals(60, resolved.participant("defender")?.currentHp)
		assertEquals(12, itemDamage.amount)
	}
}
