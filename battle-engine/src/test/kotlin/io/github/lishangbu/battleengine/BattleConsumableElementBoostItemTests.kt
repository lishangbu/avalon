package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 验证一次性属性增伤道具只在成功造成对应属性本体伤害后消费。 */
class BattleConsumableElementBoostItemTests {
	@Test
	fun `normal gem boosts normal damage and is consumed after hit`() {
		val engine = BattleEngine()
		val state = engine.start(
			initialState(
				first = participant(
					"user",
					speed = 100,
					itemId = 564,
					itemEffects = listOf(BattleItemEffect.ConsumableElementDamageBoost(1, 1.3)),
				),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", 1, "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertTrue(resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount > 28)
		assertNull(resolved.participant("user")?.itemId)
	}
}
