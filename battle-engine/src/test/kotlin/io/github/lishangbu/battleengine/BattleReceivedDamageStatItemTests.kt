package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 验证受到匹配属性或效果绝佳伤害后提升能力并消费的一次性携带道具。 */
class BattleReceivedDamageStatItemTests {
	private val engine = BattleEngine()

	@Test
	fun `weakness policy raises attack and special attack after super effective damage`() {
		val rules = neutralRules().copy(
			elementChart = ElementEffectivenessChart(mapOf(10L to mapOf(12L to 2.0))),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(elementId = 10)),
				second = participant(
					"policy-holder",
					speed = 50,
					elementId = 12,
					itemId = 639,
					itemEffects = listOf(
						BattleItemEffect.ReceivedDamageStatStageBoost(
							requiresSuperEffective = true,
							stageChanges = mapOf(BattleStat.ATTACK to 2, BattleStat.SPECIAL_ATTACK to 2),
						),
					),
				),
				rules = rules,
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "policy-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		val holder = requireNotNull(resolved.participant("policy-holder"))
		assertEquals(2, holder.statStage(BattleStat.ATTACK))
		assertEquals(2, holder.statStage(BattleStat.SPECIAL_ATTACK))
		assertNull(holder.itemId)
	}

	@Test
	fun `element reactive item raises stat only after matching element damage`() {
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(elementId = 11)),
				second = participant(
					"bulb-holder",
					speed = 50,
					itemId = 545,
					itemEffects = listOf(
						BattleItemEffect.ReceivedDamageStatStageBoost(
							elementId = 11,
							stageChanges = mapOf(BattleStat.SPECIAL_ATTACK to 1),
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "bulb-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		val holder = requireNotNull(resolved.participant("bulb-holder"))
		assertEquals(1, holder.statStage(BattleStat.SPECIAL_ATTACK))
		assertNull(holder.itemId)
	}
}
