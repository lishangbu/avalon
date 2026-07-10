package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 无视对手命中/闪避阶级变化特性的公开对照测试。
 *
 * 场景类型：命中流程 场景。
 * 参考来源类型：成熟公开对战引擎特性实现和公开命中流程实现。
 * 验证重点：持有效果的一方作为防守方时，命中判定忽略使用者命中阶级；持有效果的一方作为攻击方时，
 * 命中判定忽略目标闪避阶级。该效果不修改双方战斗快照中的真实阶级，只影响当前这次命中判定。
 */
class BattleAccuracyStatStageIgnoreAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `defender accuracy stage ignore ability ignores attacker's lowered accuracy`() {
		val scenario = publicBattleRuleScenario(
			name = "defender-accuracy-stage-ignore-ability-ignores-attacker-accuracy-drop",
			inputSummary = "使用者命中阶级为 -1，技能基础命中为 75，防守方拥有无视对手命中阶级变化特性，固定命中随机数为 75。",
			expectedSummary = "防守方特性使使用者命中阶级按 0 处理，有效命中保持 75；随机数 75 命中并继续造成伤害。",
		)
		val loweredAccuracySkill = damagingSkill(accuracy = 75)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = loweredAccuracySkill).copy(
					statStages = mapOf(BattleStat.ACCURACY to -1),
				),
				second = participant(
					"stage-ignoring-defender",
					speed = 50,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreOpponentAccuracyStatStages()),
				),
			),
		)
		val random = ScriptedBattleRandom(listOf(74, 1, 15))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "stage-ignoring-defender")),
			random,
		)

		scenario.assertNamed("defender-accuracy-stage-ignore-ability-ignores-attacker-accuracy-drop")
		assertEquals(72, resolved.participant("stage-ignoring-defender")?.currentHp)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.SkillMissed>().none())
		assertEquals(
			listOf("accuracy for 1", "critical hit for 1", "damage random for 1"),
			random.consumedReasons(),
		)
	}

	@Test
	fun `attacker accuracy stage ignore ability ignores target evasion boost`() {
		val scenario = publicBattleRuleScenario(
			name = "attacker-accuracy-stage-ignore-ability-ignores-target-evasion-boost",
			inputSummary = "目标闪避阶级为 +1，使用者拥有无视对手闪避阶级变化特性，并使用基础命中 100 的技能。",
			expectedSummary = "目标闪避阶级按 0 处理，有效命中达到 100；引擎不消耗命中随机数，技能直接命中。",
		)
		val accurateSkill = damagingSkill(accuracy = 100)
		val state = engine.start(
			initialState(
				first = participant(
					"stage-ignoring-attacker",
					speed = 100,
					skill = accurateSkill,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreOpponentAccuracyStatStages()),
				),
				second = participant("evasive-defender", speed = 50).copy(
					statStages = mapOf(BattleStat.EVASION to 1),
				),
			),
		)
		val random = ScriptedBattleRandom(listOf(1, 15))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("stage-ignoring-attacker", skillId = 1, targetActorId = "evasive-defender")),
			random,
		)

		scenario.assertNamed("attacker-accuracy-stage-ignore-ability-ignores-target-evasion-boost")
		assertEquals(72, resolved.participant("evasive-defender")?.currentHp)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.SkillMissed>().none())
		assertEquals(listOf("critical hit for 1", "damage random for 1"), random.consumedReasons())
	}
}
