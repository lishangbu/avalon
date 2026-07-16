package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleSpecializedPriorityAbilityTests {
	@Test
	fun `gale wings lets a slower full hp flying user act first`() {
		val flying = damagingSkill(skillId = 891, elementId = 3)
		val normal = damagingSkill(skillId = 892)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant(
							"holder",
							50,
							skill = flying,
							abilityEffects = listOf(BattleAbilityEffect.ElementSkillPriorityBoost(3, 1, true)),
						),
						second = participant("opponent", 100, skill = normal),
					),
				),
				listOf(
					BattleAction.UseSkill("holder", flying.skillId, "opponent"),
					BattleAction.UseSkill("opponent", normal.skillId, "holder"),
				),
				ScriptedBattleRandom(listOf(1, 15, 1, 15)),
			)
		}

		assertEquals("holder", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
	}

	@Test
	fun `triage gives healing skills plus three priority`() {
		val healing = damagingSkill(
			skillId = 893,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			hpEffects = listOf(BattleSkillHpEffect.SelfHealMaxHpFraction(1, 2)),
		)
		val attack = damagingSkill(skillId = 894)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant(
							"holder",
							50,
							currentHp = 50,
							skill = healing,
							abilityEffects = listOf(BattleAbilityEffect.HealingSkillPriorityBoost(3)),
						),
						second = participant("opponent", 100, skill = attack),
					),
				),
				listOf(
					BattleAction.UseSkill("holder", healing.skillId, "holder"),
					BattleAction.UseSkill("opponent", attack.skillId, "holder"),
				),
				ScriptedBattleRandom(listOf(1, 15)),
			)
		}

		assertEquals("holder", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
	}
}
