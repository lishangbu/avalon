package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifier
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierApplication
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierKind
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证乘风会吸收风类技能并提升自身攻击。 */
class BattleWindRiderAbilityTests {
	@Test
	fun `wind rider blocks a wind skill and raises attack`() {
		val state = BattleEngine().start(initialState(
			first = participant("user", 100, skill = damagingSkill(windBased = true)),
			second = participant(
				"holder",
				80,
				abilityEffects = listOf(BattleAbilityEffect.WindSkillImmunityStatStageChange(BattleStat.ATTACK, 1)),
			),
		))

		val resolved = BattleEngine().resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", 1, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(100, resolved.participant("holder")?.currentHp)
		assertEquals(1, resolved.participant("holder")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `wind rider raises attack when tailwind starts on its side`() {
		val tailwind = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			power = null,
			sideSpeedModifierApplications = listOf(
				BattleSideSpeedModifierApplication(
					BattleSideConditionTarget.USER_SIDE,
					BattleSideSpeedModifier(BattleSideSpeedModifierKind.TAILWIND, turnsRemaining = 4),
				),
			),
		)
		val state = BattleEngine().start(initialState(
			first = participant(
				"holder",
				100,
				skill = tailwind,
				abilityEffects = listOf(BattleAbilityEffect.WindSkillImmunityStatStageChange(BattleStat.ATTACK, 1)),
			),
			second = participant("opponent", 80),
		))

		val resolved = BattleEngine().resolveTurn(
			state,
			listOf(BattleAction.UseSkill("holder", 1, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(1, resolved.participant("holder")?.statStage(BattleStat.ATTACK))
	}
}
