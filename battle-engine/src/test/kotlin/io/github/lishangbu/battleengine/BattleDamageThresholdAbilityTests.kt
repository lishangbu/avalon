package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleDamageThresholdAbilityTests {
	@Test
	fun `anger point sets attack to plus six after surviving a critical hit`() {
		val skill = damagingSkill(skillId = 831, criticalHitStage = 3)
		val holder = participant(
			"holder",
			50,
			abilityEffects = listOf(BattleAbilityEffect.CriticalDamageSetStatStage(BattleStat.ATTACK, 6)),
		)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(initialState(first = participant("attacker", 100, skill = skill), second = holder)),
				listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
				ScriptedBattleRandom(listOf(15)),
			)
		}

		assertEquals(6, resolved.participant("holder")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `crossing half hp applies berserk and anger shell changes only once`() {
		val skill = damagingSkill(
			skillId = 832,
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(60),
		)
		val holder = participant(
			"holder",
			50,
			abilityEffects = listOf(
				BattleAbilityEffect.DamageCrossedHpThresholdStatStageChange(mapOf(BattleStat.SPECIAL_ATTACK to 1)),
				BattleAbilityEffect.DamageCrossedHpThresholdStatStageChange(
					mapOf(
						BattleStat.ATTACK to 1,
						BattleStat.SPEED to 1,
						BattleStat.DEFENSE to -1,
					),
				),
			),
		)
		val engine = BattleEngine()
		val first = engine.resolveTurn(
			engine.start(initialState(first = participant("attacker", 100, skill = skill), second = holder)),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(1, first.participant("holder")?.statStage(BattleStat.SPECIAL_ATTACK))
		assertEquals(1, first.participant("holder")?.statStage(BattleStat.ATTACK))
		assertEquals(1, first.participant("holder")?.statStage(BattleStat.SPEED))
		assertEquals(-1, first.participant("holder")?.statStage(BattleStat.DEFENSE))
	}
}
