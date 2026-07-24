package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 验证鳞粉类特性只阻止伤害技能追加效果，不阻止直接伤害。 */
class BattleSecondaryEffectImmunityAbilityTests {
	@Test
	fun `secondary effect immunity blocks damaging move stat drop but keeps damage`() {
		val skill = damagingSkill(
			statStageEffects = listOf(
				BattleStatStageEffect(BattleStat.DEFENSE, BattleEffectTarget.TARGET, -1, 100),
			),
		)
		val engine = BattleEngine()
		val state = engine.start(
			initialState(
				first = participant("attacker", 100, skill = skill),
				second = participant(
					"shield-dust-holder",
					50,
					abilityEffects = listOf(BattleAbilityEffect.DamagingSkillSecondaryEffectImmunity()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "shield-dust-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertTrue(requireNotNull(resolved.participant("shield-dust-holder")).currentHp < 100)
		assertEquals(0, resolved.participant("shield-dust-holder")?.statStage(BattleStat.DEFENSE))
	}
}
