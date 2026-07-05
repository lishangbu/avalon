package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 验证“造成伤害但不能让目标倒下”的技能族。
 *
 * 场景类型：普通公式伤害写入 HP 前的目标本体 1 HP 下限场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则中，这类技能只限制目标本体 HP 写入：伤害公式、
 * 命中、属性、接触、道具增伤和能力阶级都照常计算；如果原始伤害足以让目标倒下，实际写入伤害被夹到当前 HP - 1。
 * 它不等同于满 HP 保命特性/道具，也不保护替身，所以本测试同时固定“不会触发保命/低体力受伤 hook”和“替身可以
 * 被正常打破”的边界。
 */
class BattleNonFaintingDamageSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `non fainting damage skill leaves target at one hp`() {
		val scenario = publicBattleRuleScenario(
			name = "non-fainting-damage-skill-leaves-target-at-one-hp",
			inputSummary = "目标剩余 20 HP，受到一个足以打倒目标、但声明不能让目标倒下的普通伤害技能。",
			expectedSummary = "目标没有倒下，HP 被夹到 1；伤害事件记录真实扣除的 19 HP，战斗不会结束。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = nonFaintingSkill()),
				second = participant("target", speed = 50, currentHp = 20),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 9206, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("non-fainting-damage-skill-leaves-target-at-one-hp")
		assertEquals(1, resolved.participant("target")?.currentHp)
		assertNull(resolved.result)
		assertEquals(19, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.ParticipantFainted>())
	}

	@Test
	fun `non fainting damage skill at one hp records zero damage and skips low hp healing item`() {
		val scenario = publicBattleRuleScenario(
			name = "non-fainting-damage-skill-at-one-hp-skips-low-hp-healing-item",
			inputSummary = "目标已经只有 1 HP，携带受伤后低体力回复道具，再受到不能让目标倒下的普通伤害技能。",
			expectedSummary = "目标 HP 保持 1，伤害事件为 0；因为没有实际受到伤害，低体力回复道具不会触发也不会被消费。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = nonFaintingSkill()),
				second = participant(
					"target",
					speed = 50,
					currentHp = 1,
					itemId = 132,
					itemEffects = listOf(BattleItemEffect.LowHpHeal(fixedHealAmount = 20)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 9206, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val target = requireNotNull(resolved.participant("target"))

		scenario.assertNamed("non-fainting-damage-skill-at-one-hp-skips-low-hp-healing-item")
		assertEquals(1, target.currentHp)
		assertEquals(132, target.itemId)
		assertEquals(0, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.HealingApplied>())
	}

	@Test
	fun `non fainting damage skill can still break substitute`() {
		val scenario = publicBattleRuleScenario(
			name = "non-fainting-damage-skill-can-still-break-substitute",
			inputSummary = "目标本体 HP 充足但拥有 20 HP 替身，受到不能让目标倒下的普通伤害技能。",
			expectedSummary = "替身承受并被打破，目标本体 HP 不变；1 HP 下限只作用于目标本体，不保护替身。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = nonFaintingSkill()),
				second = participant("target", speed = 50).copy(substituteHp = 20),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 9206, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("non-fainting-damage-skill-can-still-break-substitute")
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals(0, resolved.participant("target")?.substituteHp)
		assertEquals(20, resolved.events.filterIsInstance<BattleEvent.SubstituteDamageApplied>().single().amount)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.SubstituteBroken>().size)
	}

	private fun nonFaintingSkill() =
		damagingSkill(
			skillId = 9206,
			name = "保留一血测试",
			power = 250,
			leavesTargetAtOneHp = true,
		)
}
