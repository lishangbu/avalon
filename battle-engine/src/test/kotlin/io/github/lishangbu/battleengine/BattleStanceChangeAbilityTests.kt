package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFormProfile
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 验证战斗切换会在技能结算前通过统一形态画像切换坚盾剑怪的攻防形态。 */
class BattleStanceChangeAbilityTests {
	private val shieldForm = BattleFormProfile(681, 100, 50, 140, 50, 140, 60, 530, setOf(9, 8))
	private val bladeForm = BattleFormProfile(10026, 100, 140, 50, 140, 50, 60, 530, setOf(9, 8))
	private val stanceChange = BattleAbilityEffect.StanceChange(
		defensiveFormCode = "aegislash-shield",
		offensiveFormCode = "aegislash-blade",
	)

	@Test
	fun `damaging move changes shield form into blade form before resolution`() {
		val skill = damagingSkill(skillId = 588)
		val holder = participant(
			actorId = "aegislash",
			speed = shieldForm.speed,
			skill = skill,
			creatureId = shieldForm.creatureId,
			battleFormProfiles = mapOf(
				"aegislash-shield" to shieldForm,
				"aegislash-blade" to bladeForm,
			),
			abilityEffects = listOf(stanceChange),
		)
		val state = BattleEngine().start(initialState(holder, participant("target", 40)))

		val resolved = BattleEngine().resolveTurn(
			state,
			listOf(BattleAction.UseSkill("aegislash", 588, "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		val changed = requireNotNull(resolved.participant("aegislash"))
		assertEquals(bladeForm.creatureId, changed.creatureId)
		assertEquals(bladeForm.attack, changed.attack)
		assertTrue(resolved.events.any {
			it is BattleEvent.FormChanged && it.actorId == "aegislash" && it.toCreatureId == bladeForm.creatureId
		})
	}

	@Test
	fun `defensive stance move changes blade form back into shield form`() {
		val skill = damagingSkill(
			skillId = 588,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			returnsUserToDefensiveForm = true,
		)
		val holder = participant(
			actorId = "aegislash",
			speed = bladeForm.speed,
			skill = skill,
			creatureId = bladeForm.creatureId,
			battleFormProfiles = mapOf(
				"aegislash-shield" to shieldForm,
				"aegislash-blade" to bladeForm,
			),
			abilityEffects = listOf(stanceChange),
		)
		val state = BattleEngine().start(initialState(holder, participant("target", 40)))

		val resolved = BattleEngine().resolveTurn(
			state,
			listOf(BattleAction.UseSkill("aegislash", 588, "aegislash")),
			ScriptedBattleRandom(emptyList()),
		)

		val changed = requireNotNull(resolved.participant("aegislash"))
		assertEquals(shieldForm.creatureId, changed.creatureId)
		assertEquals(shieldForm.defense, changed.defense)
	}

	@Test
	fun `switching out returns blade form to shield form`() {
		val attack = damagingSkill(skillId = 588)
		val holder = participant(
			actorId = "aegislash",
			speed = shieldForm.speed,
			skill = attack,
			creatureId = shieldForm.creatureId,
			battleFormProfiles = mapOf(
				"aegislash-shield" to shieldForm,
				"aegislash-blade" to bladeForm,
			),
			abilityEffects = listOf(stanceChange),
		)
		val engine = BattleEngine()
		val afterAttack = engine.resolveTurn(
			engine.start(initialState(holder, participant("target", 40), firstBench = listOf(participant("bench", 30)))),
			listOf(BattleAction.UseSkill("aegislash", 588, "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		val switched = engine.resolveTurn(
			afterAttack,
			listOf(BattleAction.SwitchParticipant("aegislash", "bench")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(shieldForm.creatureId, switched.participant("aegislash")?.creatureId)
	}
}
