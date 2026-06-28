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
			ScriptedBattleRandom(listOf(15)),
		)

		assertEquals("single-target-move-follows-replacement-slot", fixture.name)
		assertEquals(listOf("reserve"), resolved.sideOf("reserve")?.activeActorIds)
		assertEquals(100, resolved.participant("starter")?.currentHp)
		assertEquals(72, resolved.participant("reserve")?.currentHp)
		assertEquals("reserve", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single().targetActorId)
	}

	private data class PublicReferenceFixture(
		val name: String,
		val sourceUrls: List<String>,
		val inputSummary: String,
		val expectedSummary: String,
	)
}
