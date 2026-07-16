package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 验证强制接地与属性免疫失效类携带道具的伤害边界。 */
class BattleGroundingItemTests {
	private val engine = BattleEngine()

	@Test
	fun `air balloon grants ground immunity until holder takes damage`() {
		val normalSkill = damagingSkill(skillId = 203, name = "撞击", elementId = 1)
		val groundSkill = damagingSkill(skillId = 204, name = "地震", elementId = 5)
		val attacker = participant("attacker", speed = 100, skill = normalSkill).copy(
			skillSlots = listOf(normalSkill, groundSkill),
		)
		val state = engine.start(
			initialState(
				first = attacker,
				second = participant(
					actorId = "holder",
					speed = 80,
					itemId = 584,
					itemEffects = listOf(BattleItemEffect.AirborneUntilDamaged()),
				),
			),
		)

		val immune = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", 204, "holder")),
			ScriptedBattleRandom(emptyList()),
		)
		assertEquals(100, immune.participant("holder")?.currentHp)
		assertEquals(584, immune.participant("holder")?.itemId)

		val popped = engine.resolveTurn(
			immune,
			listOf(BattleAction.UseSkill("attacker", 203, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		assertEquals(null, popped.participant("holder")?.itemId)

		val groundedHit = engine.resolveTurn(
			popped,
			listOf(BattleAction.UseSkill("attacker", 204, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		assertTrue(requireNotNull(groundedHit.participant("holder")).currentHp < requireNotNull(popped.participant("holder")).currentHp)
	}

	@Test
	fun `iron ball grounds holder and removes flying ground immunity`() {
		val groundSkill = damagingSkill(skillId = 201, name = "地震", elementId = 5)
		val rules = neutralRules().copy(
			elementChart = ElementEffectivenessChart(mapOf(5L to mapOf(3L to 0.0))),
		)
		val holder = participant(
			actorId = "holder",
			speed = 80,
			elementId = 3,
			grounded = false,
			itemId = 1101,
			itemEffects = listOf(BattleItemEffect.GroundingOverride(), BattleItemEffect.SpeedMultiplier(0.5)),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = groundSkill),
				second = holder,
				rules = rules,
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", 201, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertTrue(requireNotNull(resolved.participant("holder")).currentHp < 100)
		assertTrue(holder.isEffectivelyGrounded())
	}

	@Test
	fun `ring target removes type immunity without being consumed`() {
		val normalSkill = damagingSkill(skillId = 202, name = "撞击", elementId = 1)
		val rules = neutralRules().copy(
			elementChart = ElementEffectivenessChart(mapOf(1L to mapOf(8L to 0.0))),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = normalSkill),
				second = participant(
					actorId = "holder",
					speed = 80,
					elementId = 8,
					itemId = 1120,
					itemEffects = listOf(BattleItemEffect.TypeImmunitySuppression()),
				),
				rules = rules,
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", 202, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		assertTrue(damage.amount > 0)
		assertEquals(1.0, damage.effectiveness)
		assertEquals(1120, resolved.participant("holder")?.itemId)
	}
}
