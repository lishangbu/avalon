package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderEffect
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证舞者会立即复制其他成员使用的舞蹈技能，且不要求自己拥有该技能。 */
class BattleDancerAbilityTests {
	@Test
	fun `dancer immediately copies a dance move without consuming a skill slot`() {
		val dance = damagingSkill(
			skillId = 483,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			danceBased = true,
			statStageEffects = listOf(BattleStatStageEffect(BattleStat.SPEED, BattleEffectTarget.USER, 1, 100)),
		)
		val state = BattleEngine().start(initialState(
			first = participant("dance-user", 100, skill = dance),
			second = participant(
				"dancer",
				80,
				abilityEffects = listOf(BattleAbilityEffect.DanceMoveCopy()),
			),
		))

		val resolved = BattleEngine().resolveTurn(
			state,
			listOf(BattleAction.UseSkill("dance-user", 483, "dance-user")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(1, resolved.participant("dance-user")?.statStage(BattleStat.SPEED))
		assertEquals(1, resolved.participant("dancer")?.statStage(BattleStat.SPEED))
	}

	@Test
	fun `multiple dancers copy a dance move in effective speed order`() {
		val dance = damagingSkill(
			skillId = 483,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			danceBased = true,
			statStageEffects = listOf(BattleStatStageEffect(BattleStat.SPEED, BattleEffectTarget.USER, 1, 100)),
		)
		val dancerEffect = listOf(BattleAbilityEffect.DanceMoveCopy())
		val state = BattleEngine().start(doubleInitialState(
			firstA = participant("dance-user", 100, skill = dance),
			firstB = participant("z-fast-dancer", 120, abilityEffects = dancerEffect),
			secondA = participant("a-slow-dancer", 40, abilityEffects = dancerEffect),
			secondB = participant("observer", 20),
		))

		val resolved = BattleEngine().resolveTurn(
			state,
			listOf(BattleAction.UseSkill("dance-user", 483, "dance-user")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(
			listOf("dance-user", "z-fast-dancer", "a-slow-dancer"),
			resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId },
		)
	}

	@Test
	fun `trick room reverses the order of multiple dancer copies`() {
		val dance = damagingSkill(
			skillId = 483,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			danceBased = true,
			statStageEffects = listOf(BattleStatStageEffect(BattleStat.SPEED, BattleEffectTarget.USER, 1, 100)),
		)
		val dancerEffect = listOf(BattleAbilityEffect.DanceMoveCopy())
		val state = BattleEngine().start(doubleInitialState(
			firstA = participant("dance-user", 100, skill = dance),
			firstB = participant("fast-dancer", 120, abilityEffects = dancerEffect),
			secondA = participant("slow-dancer", 40, abilityEffects = dancerEffect),
			secondB = participant("observer", 20),
			environment = BattleEnvironment(
				fieldSpeedOrderEffect = BattleFieldSpeedOrderEffect(BattleFieldSpeedOrderKind.TRICK_ROOM),
			),
		))

		val resolved = BattleEngine().resolveTurn(
			state,
			listOf(BattleAction.UseSkill("dance-user", 483, "dance-user")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(
			listOf("dance-user", "slow-dancer", "fast-dancer"),
			resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId },
		)
	}
}
