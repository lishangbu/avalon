package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证魔法镜会把面向持有者的变化技能反射给原使用者。 */
class BattleMagicBounceAbilityTests {
	@Test
	fun `magic bounce reflects an opponent targeted status skill`() {
		val skill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(BattleMajorStatus.POISON, BattleEffectTarget.TARGET, 100),
			),
		)
		val state = BattleEngine().start(initialState(
			first = participant("user", 100, skill = skill),
			second = participant(
				"holder",
				80,
				abilityEffects = listOf(BattleAbilityEffect.OpponentStatusSkillReflection()),
			),
		))

		val resolved = BattleEngine().resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", 1, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(BattleMajorStatus.POISON, resolved.participant("user")?.majorStatus)
		assertEquals(null, resolved.participant("holder")?.majorStatus)
		assertEquals("holder", resolved.events.filterIsInstance<BattleEvent.SkillReflected>().single().actorId)
	}
}
