package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleEndTurnAbilityTests {
	@Test
	fun `speed boost raises speed at end of turn`() {
		val firstSkill = protectionSkill(801)
		val secondSkill = protectionSkill(802)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"holder",
						100,
						skill = firstSkill,
						abilityEffects = listOf(BattleAbilityEffect.EndTurnStatStageChange(BattleStat.SPEED, 1)),
					),
					second = participant("target", 50, skill = secondSkill),
				),
			),
			listOf(
				BattleAction.UseSkill("holder", firstSkill.skillId, "holder"),
				BattleAction.UseSkill("target", secondSkill.skillId, "target"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(1, resolved.participant("holder")?.statStage(BattleStat.SPEED))
	}

	@Test
	fun `poison heal replaces poison damage with one eighth healing`() {
		val firstSkill = protectionSkill(811)
		val secondSkill = protectionSkill(812)
		val engine = BattleEngine()
		val holder = participant(
			"holder",
			100,
			currentHp = 50,
			skill = firstSkill,
			abilityEffects = listOf(
				BattleAbilityEffect.MajorStatusEndTurnHeal(
					setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
					8,
				),
			),
		).copy(majorStatus = BattleMajorStatus.POISON)
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = holder, second = participant("target", 50, skill = secondSkill))),
			listOf(
				BattleAction.UseSkill("holder", firstSkill.skillId, "holder"),
				BattleAction.UseSkill("target", secondSkill.skillId, "target"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(62, resolved.participant("holder")?.currentHp)
		assertEquals(BattleMajorStatus.POISON, resolved.participant("holder")?.majorStatus)
	}
}
