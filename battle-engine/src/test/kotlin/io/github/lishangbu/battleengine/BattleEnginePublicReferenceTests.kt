package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
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
		val fixture = PublicReferenceFixture(
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

		assertEquals("level-50-neutral-same-element-physical-damage", fixture.name)
		assertEquals(19, result.baseDamage)
		assertEquals(1.5, result.sameElementBonus)
		assertEquals(28, result.amount)
	}

	@Test
	fun `modern critical hit damage matches public calculator fixture`() {
		val fixture = PublicReferenceFixture(
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

		assertEquals("level-50-neutral-same-element-critical-hit-damage", fixture.name)
		assertEquals(19, result.baseDamage)
		assertEquals(1.5, result.sameElementBonus)
		assertEquals(1.5, result.criticalHitMultiplier)
		assertEquals(42, result.amount)
	}

	@Test
	fun `target slot follows switched in participant like public simulator fixture`() {
		val fixture = PublicReferenceFixture(
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

		assertEquals("single-target-move-follows-replacement-slot", fixture.name)
		assertEquals(listOf("reserve"), resolved.sideOf("reserve")?.activeActorIds)
		assertEquals(100, resolved.participant("starter")?.currentHp)
		assertEquals(72, resolved.participant("reserve")?.currentHp)
		assertEquals("reserve", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single().targetActorId)
	}

	@Test
	fun `protection blocks ordinary target move like public simulator fixture`() {
		val fixture = PublicReferenceFixture(
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

		assertEquals("protect-move-blocks-ordinary-target-move", fixture.name)
		assertEquals(100, resolved.participant("protector")?.currentHp)
		assertEquals(9, resolved.participant("protector")?.skillSlot(2)?.remainingPp)
		assertEquals(34, resolved.participant("attacker")?.skillSlot(1)?.remainingPp)
		assertEquals("protector", resolved.events.filterIsInstance<BattleEvent.ProtectionStarted>().single().actorId)
		assertEquals("protector", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single().targetActorId)
	}

	@Test
	fun `consecutive protection uses public simulator stalling chance fixture`() {
		val fixture = PublicReferenceFixture(
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

		assertEquals("consecutive-protection-second-use-one-third-success", fixture.name)
		assertEquals(2, resolved.participant("protector")?.protectionChain)
		assertEquals(100, resolved.participant("protector")?.currentHp)
		assertEquals("protector", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single().targetActorId)
	}

	private data class PublicReferenceFixture(
		val name: String,
		val sourceUrls: List<String>,
		val inputSummary: String,
		val expectedSummary: String,
	)
}
