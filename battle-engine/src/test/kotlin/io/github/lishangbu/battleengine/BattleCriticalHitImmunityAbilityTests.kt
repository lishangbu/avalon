package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 击中要害免疫特性的公开对照测试。
 *
 * 场景类型：特性规则 场景。
 * 参考来源类型：成熟公开对战引擎特性实现和公开伤害流程实现。
 * 验证重点：目标拥有该特性时，普通随机要害和必定要害技能都会在最终伤害请求中降回非要害；
 * 既不会应用 1.5 倍要害倍率，也不会触发要害绕过屏障等后续要害语义。
 */
class BattleCriticalHitImmunityAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `critical hit immunity blocks guaranteed critical hit damage`() {
		val scenario = publicBattleRuleScenario(
			name = "critical-hit-immunity-blocks-guaranteed-critical-hit-damage",
			inputSummary = "使用者使用基础要害等级达到必定要害的物理技能，目标拥有击中要害免疫特性。",
			expectedSummary = "技能仍命中并造成普通直接伤害，但伤害事件中的 criticalHit 为 false，最终伤害不乘以要害倍率。",
		)
		val guaranteedCriticalSkill = damagingSkill(criticalHitStage = 3)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = guaranteedCriticalSkill),
				second = participant(
					"protected-defender",
					speed = 50,
					abilityEffects = listOf(BattleAbilityEffect.CriticalHitImmunity()),
				),
			),
		)
		val random = ScriptedBattleRandom(listOf(15))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "protected-defender")),
			random,
		)

		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		scenario.assertNamed("critical-hit-immunity-blocks-guaranteed-critical-hit-damage")
		assertEquals(false, damage.criticalHit)
		assertEquals(28, damage.amount)
		assertEquals(72, resolved.participant("protected-defender")?.currentHp)
		assertEquals(listOf("damage random for 1"), random.consumedReasons())
	}

	@Test
	fun `critical hit immunity blocks successful random critical hit`() {
		val scenario = publicBattleRuleScenario(
			name = "critical-hit-immunity-blocks-successful-random-critical-hit",
			inputSummary = "普通要害等级技能的要害随机数成功，目标拥有击中要害免疫特性。",
			expectedSummary = "要害随机数仍被消费并命中成功，但目标特性让最终伤害按非要害计算。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant(
					"protected-defender",
					speed = 50,
					abilityEffects = listOf(BattleAbilityEffect.CriticalHitImmunity()),
				),
			),
		)
		val random = ScriptedBattleRandom(listOf(0, 15))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "protected-defender")),
			random,
		)

		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		scenario.assertNamed("critical-hit-immunity-blocks-successful-random-critical-hit")
		assertEquals(false, damage.criticalHit)
		assertEquals(28, damage.amount)
		assertEquals(72, resolved.participant("protected-defender")?.currentHp)
		assertEquals(listOf("critical hit for 1", "damage random for 1"), random.consumedReasons())
	}
}
