package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 公开对照 fixture 测试入口。
 *
 * 这些测试不是为了覆盖所有本地分支，而是把已经实现的规则钉到公开成熟实现或公开规则用例上。
 * 每个 fixture 都记录来源 URL、输入摘要和期望结果。后续接入天气、场地、保护、击中要害、双打范围技能
 * 等规则时，优先在这里补一条对照，再回到状态机单元测试补边界。
 */
class BattleEnginePublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `ordinary same element physical damage matches public calculator fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "level-50-neutral-same-element-physical-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/damage-calc/blob/master/calc/src/mechanics/gen789.ts",
				"https://github.com/smogon/damage-calc/blob/master/calc/src/mechanics/util.ts",
			),
			inputSummary = "等级 50，威力 40，攻击/防御 100，同属性，属性克制 1x，随机浮动 100%。",
			expectedSummary = "基础伤害 19，同属性倍率 1.5，最终伤害 28。",
		)

		val result = BattleDamageCalculator().calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100),
				defender = participant("defender", speed = 80),
				skill = damagingSkill(power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("level-50-neutral-same-element-physical-damage")
		assertEquals(19, result.baseDamage)
		assertEquals(1.5, result.sameElementBonus)
		assertEquals(28, result.amount)
	}

	@Test
	fun `modern critical hit damage matches public calculator fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "level-50-neutral-same-element-critical-hit-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/damage-calc/blob/master/calc/src/mechanics/gen789.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Critical_hit",
			),
			inputSummary = "等级 50，威力 40，攻击/防御 100，同属性，属性克制 1x，随机浮动 100%，击中要害。",
			expectedSummary = "基础伤害 19，同属性倍率 1.5，现代击中要害倍率 1.5，最终伤害 42。",
		)

		val result = BattleDamageCalculator().calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100),
				defender = participant("defender", speed = 80),
				skill = damagingSkill(power = 40),
				rules = neutralRules(),
				randomPercent = 100,
				criticalHit = true,
			),
		)

		fixture.assertNamed("level-50-neutral-same-element-critical-hit-damage")
		assertEquals(19, result.baseDamage)
		assertEquals(1.5, result.sameElementBonus)
		assertEquals(1.5, result.criticalHitMultiplier)
		assertEquals(42, result.amount)
	}

	@Test
	fun `double battle spread damage modifier matches public rule fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "double-battle-spread-damage-uses-three-quarter-target-modifier",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Damage",
				"https://github.com/smogon/pokemon-showdown/blob/master/sim/dex-moves.ts",
			),
			inputSummary = "双打中一名行动者使用能影响对方两个上场成员的物理伤害技能，两个目标均可被命中。",
			expectedSummary = "每个目标独立结算命中/要害/伤害随机数，并在普通伤害公式中使用 0.75 目标倍率。",
		)
		val spreadSkill = damagingSkill(
			name = "范围攻击",
			targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS,
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("spread-user", speed = 100, skill = spreadSkill),
				firstB = participant("ally", speed = 90),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("spread-user", skillId = 1, targetActorId = "opponent-left")),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)
		val damageEvents = resolved.events.filterIsInstance<BattleEvent.DamageApplied>()

		fixture.assertNamed("double-battle-spread-damage-uses-three-quarter-target-modifier")
		assertEquals(listOf("opponent-left", "opponent-right"), damageEvents.map { it.targetActorId })
		assertEquals(listOf(21, 21), damageEvents.map { it.amount })
		assertEquals(listOf(0.75, 0.75), damageEvents.map { it.targetMultiplier })
		assertEquals(79, resolved.participant("opponent-left")?.currentHp)
		assertEquals(79, resolved.participant("opponent-right")?.currentHp)
		assertEquals(100, resolved.participant("ally")?.currentHp)
	}

	@Test
	fun `side damage reduction uses public double battle screen modifier`() {
		val fixture = publicBattleRuleFixture(
			name = "double-battle-side-screen-uses-two-thirds-damage-modifier",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Reflect_(move)",
				"https://bulbapedia.bulbagarden.net/wiki/Light_Screen_(move)",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
			),
			inputSummary = "双打中目标方有两名可战斗上场成员，并存在影响物理伤害的防守方屏障。",
			expectedSummary = "非要害普通物理伤害按约 2/3 的防守方屏障倍率结算，不与目标范围倍率混淆。",
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("attacker", speed = 100),
				firstB = participant("ally", speed = 90),
				secondA = participant("defender-left", speed = 50),
				secondB = participant("defender-right", speed = 40),
				secondSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender-left")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("double-battle-side-screen-uses-two-thirds-damage-modifier")
		assertEquals(81, resolved.participant("defender-left")?.currentHp)
	}

	@Test
	fun `low hp berry heals once after damage like public item fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "low-hp-berry-heals-once-after-damage",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Oran_Berry",
				"https://bulbapedia.bulbagarden.net/wiki/Sitrus_Berry",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
			),
			inputSummary = "持有者最大 HP 100，当前 HP 60，受到 28 点普通伤害后 HP 降到 32，达到半血触发线。",
			expectedSummary = "一次性回复道具立即回复 10 点并被消费，成员最终 HP 为 42，后续不再保留携带道具效果。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant(
					"holder",
					speed = 50,
					currentHp = 60,
					itemId = 132,
					itemEffects = listOf(BattleItemEffect.LowHpHeal(fixedHealAmount = 10)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val holder = requireNotNull(resolved.participant("holder"))

		fixture.assertNamed("low-hp-berry-heals-once-after-damage")
		assertEquals(42, holder.currentHp)
		assertEquals(null, holder.itemId)
		assertEquals(emptyList(), holder.itemEffects)
		assertEquals(10, resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single().amount)
	}

	@Test
	fun `choice speed item modifies action order like public item fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "choice-speed-item-modifies-action-order",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-queue.ts",
			),
			inputSummary = "速度 60 的成员持有讲究类速度道具，速度 80 的对手同优先度使用普通技能。",
			expectedSummary = "持有者有效速度按 1.5 倍变为 90，因此先于速度 80 的对手行动。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"choice-user",
					speed = 60,
					itemEffects = listOf(BattleItemEffect.ChoiceSkillLock(speedMultiplier = 1.5)),
				),
				second = participant("opponent", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("choice-user", skillId = 1, targetActorId = "opponent"),
				BattleAction.UseSkill("opponent", skillId = 1, targetActorId = "choice-user"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)
		val damageEvents = resolved.events.filterIsInstance<BattleEvent.DamageApplied>()

		fixture.assertNamed("choice-speed-item-modifies-action-order")
		assertEquals(listOf("choice-user", "opponent"), damageEvents.map { it.actorId })
	}

	@Test
	fun `target slot follows switched in participant like public simulator fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "single-target-move-follows-replacement-slot",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-queue.ts",
			),
			inputSummary = "单打中一方主动替换，另一方本回合选择攻击原上场成员所在目标槽位。",
			expectedSummary = "替换先结算，随后技能命中同一槽位的新上场成员。",
		)
		val state = engine.start(
			initialState(
				first = participant("starter", speed = 100),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("attacker", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.SwitchParticipant("starter", targetActorId = "reserve"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "starter"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("single-target-move-follows-replacement-slot")
		assertEquals(listOf("reserve"), resolved.sideOf("reserve")?.activeActorIds)
		assertEquals(100, resolved.participant("starter")?.currentHp)
		assertEquals(72, resolved.participant("reserve")?.currentHp)
		assertEquals("reserve", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single().targetActorId)
	}

	@Test
	fun `protection blocks ordinary target move like public simulator fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "protect-move-blocks-ordinary-target-move",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
			),
			inputSummary = "保护类变化技能以更高优先度先行动，同回合普通单体攻击以被保护成员为目标。",
			expectedSummary = "保护屏障建立后，受保护影响的普通攻击被阻挡，目标 HP 不变化，双方仍正常消耗 PP。",
		)
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 50, skill = protectionSkill()),
				second = participant("attacker", speed = 100),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "protector"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("protect-move-blocks-ordinary-target-move")
		assertEquals(100, resolved.participant("protector")?.currentHp)
		assertEquals(9, resolved.participant("protector")?.skillSlot(2)?.remainingPp)
		assertEquals(34, resolved.participant("attacker")?.skillSlot(1)?.remainingPp)
		assertEquals("protector", resolved.events.filterIsInstance<BattleEvent.ProtectionStarted>().single().actorId)
		assertEquals("protector", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single().targetActorId)
	}

	@Test
	fun `consecutive protection uses public simulator stalling chance fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "consecutive-protection-second-use-one-third-success",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
			),
			inputSummary = "保护类行动第一回合成功后，下一回合再次使用保护类行动。",
			expectedSummary = "第二次连续保护按 1/3 成功率判定；掷中成功后继续阻挡本回合普通攻击。",
		)
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 50, skill = protectionSkill()),
				second = participant("attacker", speed = 100),
			),
		)
		val afterFirst = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker")),
			ScriptedBattleRandom(emptyList()),
		)

		val resolved = engine.resolveTurn(
			afterFirst,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "protector"),
			),
			ScriptedBattleRandom(listOf(0)),
		)

		fixture.assertNamed("consecutive-protection-second-use-one-third-success")
		assertEquals(2, resolved.participant("protector")?.protectionChain)
		assertEquals(100, resolved.participant("protector")?.currentHp)
		assertEquals("protector", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single().targetActorId)
	}

}
