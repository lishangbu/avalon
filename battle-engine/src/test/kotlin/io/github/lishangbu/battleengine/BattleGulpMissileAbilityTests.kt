package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleFormProfile
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证一口导弹的装填形态选择和两类反击结果。 */
class BattleGulpMissileAbilityTests {
	@Test
	fun `gulping form retaliation damages attacker and lowers defense`() {
		val (engine, started, surf, attack) = scenario(currentHp = 100)
		val loaded = engine.resolveTurn(
			started,
			listOf(BattleAction.UseSkill("cramorant", surf.skillId, "attacker")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val resolved = engine.resolveTurn(
			loaded,
			listOf(BattleAction.UseSkill("attacker", attack.skillId, "cramorant")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(10182, loaded.participant("cramorant")?.creatureId)
		assertEquals(845, resolved.participant("cramorant")?.creatureId)
		assertEquals(requireNotNull(loaded.participant("attacker")?.currentHp) - 25, resolved.participant("attacker")?.currentHp)
		assertEquals(-1, resolved.participant("attacker")?.statStage(BattleStat.DEFENSE))
	}

	@Test
	fun `gorging form retaliation damages and paralyzes attacker`() {
		val (engine, started, surf, attack) = scenario(currentHp = 50)
		val loaded = engine.resolveTurn(
			started,
			listOf(BattleAction.UseSkill("cramorant", surf.skillId, "attacker")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val resolved = engine.resolveTurn(
			loaded,
			listOf(BattleAction.UseSkill("attacker", attack.skillId, "cramorant")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(10183, loaded.participant("cramorant")?.creatureId)
		assertEquals(requireNotNull(loaded.participant("attacker")?.currentHp) - 25, resolved.participant("attacker")?.currentHp)
		assertEquals(BattleMajorStatus.PARALYSIS, resolved.participant("attacker")?.majorStatus)
	}

	private fun scenario(currentHp: Int): Scenario {
		val normal = form(845)
		val gulping = form(10182)
		val gorging = form(10183)
		val surf = damagingSkill(skillId = 57)
		val attack = damagingSkill(skillId = 1)
		val effects = listOf(
			BattleAbilityEffect.PostSkillHpFormChange(setOf(57, 291), "cramorant", "cramorant-gulping", "cramorant-gorging"),
			BattleAbilityEffect.ReceivedDamageFormRetaliation("cramorant-gulping", "cramorant", 4, BattleStat.DEFENSE, -1),
			BattleAbilityEffect.ReceivedDamageFormRetaliation(
				"cramorant-gorging", "cramorant", 4, attackerMajorStatus = BattleMajorStatus.PARALYSIS,
			),
		)
		val cramorant = participant("cramorant", 100, currentHp = currentHp, creatureId = normal.creatureId,
			skill = surf, abilityEffects = effects,
			battleFormProfiles = mapOf("cramorant" to normal, "cramorant-gulping" to gulping, "cramorant-gorging" to gorging))
		val engine = BattleEngine()
		return Scenario(engine, engine.start(initialState(cramorant, participant("attacker", 50, skill = attack))), surf, attack)
	}

	private fun form(creatureId: Long) = BattleFormProfile(creatureId, 100, 100, 100, 100, 100, 85, 180, setOf(11, 3))

	private data class Scenario(
		val engine: BattleEngine,
		val state: io.github.lishangbu.battleengine.model.BattleState,
		val surf: io.github.lishangbu.battleengine.model.BattleSkillSlot,
		val attack: io.github.lishangbu.battleengine.model.BattleSkillSlot,
	)
}
