package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证束缚来源携带道具对持续时间和回合末伤害的修正。 */
class BattleBindingItemTests {
	private val engine = BattleEngine()

	@Test
	fun `grip claw fixes binding duration at seven turns`() {
		val skill = damagingSkill(
			skillId = 20,
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(20),
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(BattleVolatileStatus.BINDING, BattleEffectTarget.TARGET, 100),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("binder", speed = 100, skill = skill, itemId = 263, itemEffects = listOf(BattleItemEffect.BindingDurationOverride(7))),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("binder", 20, "target")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(6, resolved.participant("target")?.bindingTurnsRemaining)
	}

	@Test
	fun `binding band increases end turn binding damage to one sixth`() {
		val state = engine.start(
			initialState(
				first = participant("binder", speed = 100, itemId = 587, itemEffects = listOf(BattleItemEffect.BindingDamageDenominator(6))),
				second = participant("target", speed = 50).copy(boundByActorId = "binder", bindingTurnsRemaining = 3),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		assertEquals(84, resolved.participant("target")?.currentHp)
	}
}
