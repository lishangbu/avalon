package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证属性免疫时的伤害和附加效果边界。
 *
 * 场景类型：属性免疫 fixture。
 * 参考来源类型：现代主系列“没有效果”规则；本测试固定一个 0 倍属性克制表，只验证引擎不会继续结算
 * 要害、伤害浮动、接触特性、反伤或技能附加效果。
 * 验证重点：免疫短路发生在伤害随机之前，并且不会把“命中流程发生过”误当成成功造成效果。
 */
class BattleImmunityTests {
	private val engine = BattleEngine()

	@Test
	fun `immune target takes zero damage and skips contact and secondary effects`() {
		val random = ScriptedBattleRandom(emptyList())
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					elementId = 1,
					skill = damagingSkill(
						elementId = 1,
						makesContact = true,
						statusApplications = listOf(
							BattleStatusApplication(
								status = BattleMajorStatus.BURN,
								target = BattleEffectTarget.TARGET,
								chancePercent = 100,
							),
						),
					),
				),
				second = participant(
					"defender",
					speed = 80,
					elementId = 2,
					abilityEffects = listOf(
						BattleAbilityEffect.ContactStatusOnAttacker(
							status = BattleMajorStatus.PARALYSIS,
							chancePercent = 100,
						),
					),
				),
				rules = BattleRuleSnapshot(
					elementChart = ElementEffectivenessChart(mapOf(1L to mapOf(2L to 0.0))),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)

		val damageEvent = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		assertEquals(0, damageEvent.amount)
		assertEquals(0.0, damageEvent.effectiveness)
		assertEquals(100, resolved.participant("defender")?.currentHp)
		assertEquals(null, resolved.participant("defender")?.majorStatus)
		assertEquals(null, resolved.participant("attacker")?.majorStatus)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
		assertEquals(emptyList(), random.consumedReasons())
	}
}
