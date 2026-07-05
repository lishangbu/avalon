package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 验证寄生种子的现代持续状态规则。
 *
 * 场景类型：变化技能命中后写入目标身上的持续状态，并在后续回合末按固定顺序造成间接伤害和来源站位回复。
 * 参考来源类型：公开规则资料和成熟公开对战引擎行为。现代规则下，寄生种子会被草属性和替身阻止；目标已经
 * 被寄生时不会刷新或覆盖来源；目标离场时解除；回合末扣血发生在主要异常和束缚之后、天气伤害之前；回复流向
 * 原使用者所在的一侧上场席位，而不是原使用者 actorId。
 *
 * 这些测试刻意覆盖双打换人后的站位语义，因为只用单打 actorId 建模也能通过“种下后下一回合扣血”的简单用例，
 * 但会在真实双打中把回复错误地给已经离场的成员或同侧另一个站位。
 */
class BattleLeechSeedTests {
	private val engine = BattleEngine()

	@Test
	fun `leech seed plants target then drains target and heals source position`() {
		val scenario = publicBattleRuleScenario(
			name = "leech-seed-plants-target-then-drains-and-heals-source-position",
			inputSummary = "受伤使用者成功对普通目标使用寄生种子。",
			expectedSummary = "目标被种下寄生种子，回合末损失最大 HP 的 1/8，使用者按同数值回复。",
		)
		val user = participant("seed-user", speed = 100, currentHp = 50, skill = leechSeedSkill())
		val target = participant("seed-target", speed = 50)

		val resolved = engine.resolveTurn(
			engine.start(initialState(first = user, second = target)),
			listOf(BattleAction.UseSkill("seed-user", skillId = 73, targetActorId = "seed-target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("leech-seed-plants-target-then-drains-and-heals-source-position")
		assertEquals(62, resolved.participant("seed-user")?.currentHp)
		assertEquals(88, resolved.participant("seed-target")?.currentHp)
		assertEquals("side-a", resolved.participant("seed-target")?.leechSeedSourceSideId)
		assertEquals(0, resolved.participant("seed-target")?.leechSeedSourceActiveIndex)
		assertEquals("seed-target", resolved.events.filterIsInstance<BattleEvent.LeechSeedPlanted>().single().targetActorId)
		assertEquals(12, resolved.events.filterIsInstance<BattleEvent.LeechSeedDamageApplied>().single().amount)
		assertEquals(12, resolved.events.filterIsInstance<BattleEvent.LeechSeedHealingApplied>().single().amount)
	}

	@Test
	fun `leech seed fails against current grass element target without planting state`() {
		val scenario = publicBattleRuleScenario(
			name = "leech-seed-fails-against-current-grass-element-target",
			inputSummary = "使用者尝试对当前草属性目标使用寄生种子。",
			expectedSummary = "草属性免疫阻止寄生种子写入，回合末不会产生寄生种子伤害或回复。",
		)
		val target = participant("grass-target", speed = 50, elementId = 12)

		val resolved = engine.resolveTurn(
			engine.start(initialState(first = participant("seed-user", speed = 100, skill = leechSeedSkill()), second = target)),
			listOf(BattleAction.UseSkill("seed-user", skillId = 73, targetActorId = "grass-target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("leech-seed-fails-against-current-grass-element-target")
		assertFalse(requireNotNull(resolved.participant("grass-target")).isLeechSeeded())
		assertEquals("grass-target-immune-to-leech-seed", resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.LeechSeedPlanted>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.LeechSeedDamageApplied>())
	}

	@Test
	fun `leech seed is blocked by opponent substitute before planting state`() {
		val scenario = publicBattleRuleScenario(
			name = "leech-seed-is-blocked-by-opponent-substitute",
			inputSummary = "目标已经拥有替身，对手尝试使用寄生种子。",
			expectedSummary = "替身阻止寄生种子写入，目标本体和替身 HP 都保持不变。",
		)
		val target = participant("protected-target", speed = 50, currentHp = 75).copy(substituteHp = 25)

		val resolved = engine.resolveTurn(
			engine.start(initialState(first = participant("seed-user", speed = 100, skill = leechSeedSkill()), second = target)),
			listOf(BattleAction.UseSkill("seed-user", skillId = 73, targetActorId = "protected-target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("leech-seed-is-blocked-by-opponent-substitute")
		assertEquals(75, resolved.participant("protected-target")?.currentHp)
		assertEquals(25, resolved.participant("protected-target")?.substituteHp)
		assertFalse(requireNotNull(resolved.participant("protected-target")).isLeechSeeded())
		assertEquals("leech-seed-blocked-by-substitute", resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.LeechSeedPlanted>())
	}

	@Test
	fun `leech seed heals replacement occupying original source position`() {
		val scenario = publicBattleRuleScenario(
			name = "leech-seed-heals-replacement-occupying-original-source-position",
			inputSummary = "双打中左侧使用者种下寄生种子后换下，后备成员进入同一上场席位。",
			expectedSummary = "目标继续被寄生种子抽取 HP，回复给原使用者所在席位的新上场成员。",
		)
		val seedUser = participant("seed-user", speed = 100, skill = leechSeedSkill())
		val ally = participant("ally", speed = 90)
		val reserve = participant("reserve", speed = 80, currentHp = 50)
		val target = participant("seed-target", speed = 70)
		val observer = participant("observer", speed = 60)
		val initial = doubleInitialState(firstA = seedUser, firstB = ally, secondA = target, secondB = observer)
		val withReserve = initial.copy(
			sides = initial.sides.map { side ->
				if (side.sideId == "side-a") side.copy(participants = side.participants + reserve) else side
			},
		)

		val afterPlant = engine.resolveTurn(
			engine.start(withReserve),
			listOf(BattleAction.UseSkill("seed-user", skillId = 73, targetActorId = "seed-target")),
			ScriptedBattleRandom(emptyList()),
		)
		val afterSwitch = engine.resolveTurn(
			afterPlant,
			listOf(BattleAction.SwitchParticipant("seed-user", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("leech-seed-heals-replacement-occupying-original-source-position")
		assertEquals(76, afterSwitch.participant("seed-target")?.currentHp)
		assertEquals(62, afterSwitch.participant("reserve")?.currentHp)
		assertEquals(listOf("reserve", "ally"), afterSwitch.sides.single { it.sideId == "side-a" }.activeActorIds)
		assertEquals(
			listOf("seed-target" to 12, "seed-target" to 12),
			afterSwitch.events.filterIsInstance<BattleEvent.LeechSeedDamageApplied>().map { it.actorId to it.amount },
		)
		assertEquals("reserve", afterSwitch.events.filterIsInstance<BattleEvent.LeechSeedHealingApplied>().last().actorId)
	}

	@Test
	fun `leech seed is cleared when seeded target leaves battlefield`() {
		val scenario = publicBattleRuleScenario(
			name = "leech-seed-is-cleared-when-seeded-target-leaves-battlefield",
			inputSummary = "目标被寄生种子命中后，下一回合主动替换到后备成员。",
			expectedSummary = "离场目标清除寄生种子，换入成员不会继承寄生种子，也不会在回合末被抽取 HP。",
		)
		val target = participant("seed-target", speed = 50)
		val reserve = participant("target-reserve", speed = 40)

		val afterPlant = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("seed-user", speed = 100, skill = leechSeedSkill()),
					second = target,
					secondBench = listOf(reserve),
				),
			),
			listOf(BattleAction.UseSkill("seed-user", skillId = 73, targetActorId = "seed-target")),
			ScriptedBattleRandom(emptyList()),
		)
		val afterSwitch = engine.resolveTurn(
			afterPlant,
			listOf(BattleAction.SwitchParticipant("seed-target", targetActorId = "target-reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("leech-seed-is-cleared-when-seeded-target-leaves-battlefield")
		assertFalse(requireNotNull(afterSwitch.participant("seed-target")).isLeechSeeded())
		assertFalse(requireNotNull(afterSwitch.participant("target-reserve")).isLeechSeeded())
		assertEquals(88, afterSwitch.participant("seed-target")?.currentHp)
		assertEquals(100, afterSwitch.participant("target-reserve")?.currentHp)
		assertEquals(1, afterSwitch.events.filterIsInstance<BattleEvent.LeechSeedDamageApplied>().size)
	}

	private fun leechSeedSkill(): io.github.lishangbu.battleengine.model.BattleSkillSlot =
		damagingSkill(
			skillId = 73,
			name = "寄生种子",
			elementId = 12,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			plantsLeechSeed = true,
		).copy(remainingPp = 10, maxPp = 10)
}
