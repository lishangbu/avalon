package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillWeightEffect
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证技能造成的临时体重变化。
 *
 * 场景类型：技能附加效果 场景。
 * 参考来源类型：公开成熟对战引擎技能资料与公开技能规则说明。
 * 验证重点：体重降低只在同一次技能成功改变指定能力阶级后触发；降低量按资料体重刻度保存，最低有效体重为
 * 0.1kg；离场会清除该临时减重，避免下一次上场继续影响低踢、打草结、重磅冲撞和高温重压。
 */
class BattleSkillWeightEffectTests {
	private val engine = BattleEngine()

	@Test
	fun `weight reduction applies after required speed stage changes`() {
		val scenario = publicBattleRuleScenario(
			name = "weight-reduction-applies-after-required-speed-stage-changes",
			inputSummary = "使用者成功把自身速度阶级提升 2 级，并声明技能成功后降低 100kg 当前体重。",
			expectedSummary = "速度阶级变化事件先产生，随后累计体重减轻量增加 1000 个资料刻度。",
		)
		val skill = weightReductionSkill()
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, weight = 5000, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 475, targetActorId = "actor")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("weight-reduction-applies-after-required-speed-stage-changes")
		assertEquals(2, resolved.participant("actor")?.statStage(BattleStat.SPEED))
		assertEquals(1000, resolved.participant("actor")?.weightReduction)
		assertEquals(BattleEffectiveWeight(numerator = 4000, denominator = 1), resolved.participant("actor")!!.effectiveWeight())
		assertEquals(
			listOf(
				BattleEvent.StatStageChanged::class,
				BattleEvent.WeightReductionChanged::class,
			),
			resolved.events
				.filter { it is BattleEvent.StatStageChanged || it is BattleEvent.WeightReductionChanged }
				.map { it::class },
		)
	}

	@Test
	fun `weight reduction does not apply when required speed stage is unchanged`() {
		val scenario = publicBattleRuleScenario(
			name = "weight-reduction-does-not-apply-when-required-speed-stage-is-unchanged",
			inputSummary = "使用者速度阶级已经到 +6，再次使用同一个速度提升并减重的技能。",
			expectedSummary = "速度阶级没有实际变化，因此不会继续降低当前体重，也不会产生体重变化事件。",
		)
		val skill = weightReductionSkill()
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, weight = 5000, skill = skill).copy(
					statStages = mapOf(BattleStat.SPEED to 6),
				),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 475, targetActorId = "actor")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("weight-reduction-does-not-apply-when-required-speed-stage-is-unchanged")
		assertEquals(6, resolved.participant("actor")?.statStage(BattleStat.SPEED))
		assertEquals(0, resolved.participant("actor")?.weightReduction)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.WeightReductionChanged>())
	}

	@Test
	fun `leaving battlefield clears temporary weight reduction`() {
		val participant = participant("actor", speed = 100, weight = 5000, weightReduction = 1000)

		assertEquals(0, participant.leaveBattlefield().weightReduction)
	}

	@Test
	fun `effective weight applies temporary reduction before ability and item multipliers`() {
		val participant = participant("actor", speed = 100, weight = 1500, weightReduction = 1000)
			.replaceAbilityEffects(listOf(BattleAbilityEffect.WeightMultiplier(numerator = 2, denominator = 1)))
			.replaceItemEffects(listOf(BattleItemEffect.WeightMultiplier(numerator = 1, denominator = 2)))

		assertEquals(BattleEffectiveWeight(numerator = 1000, denominator = 2), participant.effectiveWeight())
	}

	private fun weightReductionSkill() =
		damagingSkill(
			skillId = 475,
			name = "身体轻量化",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = null,
			targetScope = BattleSkillTargetScope.SELF,
			affectedByProtect = false,
			statStageEffects = listOf(
				BattleStatStageEffect(
					target = BattleEffectTarget.USER,
					stat = BattleStat.SPEED,
					stageDelta = 2,
					chancePercent = 100,
				),
			),
			weightEffects = listOf(
				BattleSkillWeightEffect(
					target = BattleEffectTarget.USER,
					reduction = 1000,
					minimumWeight = 1,
					requiredChangedStat = BattleStat.SPEED,
				),
			),
		)
}
