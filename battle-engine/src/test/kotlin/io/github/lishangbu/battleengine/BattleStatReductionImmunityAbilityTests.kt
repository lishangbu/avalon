package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证能力下降免疫对技能与入场特性的阻挡边界。 */
class BattleStatReductionImmunityAbilityTests {
	private val immunity = BattleAbilityEffect.OpponentStatStageReductionImmunity(BattleStat.entries.toSet())

	@Test
	fun `stat reduction immunity blocks skills and switch in abilities`() {
		val skill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statStageEffects = listOf(BattleStatStageEffect(BattleStat.DEFENSE, BattleEffectTarget.TARGET, -1, 100)),
		)
		val engine = BattleEngine()
		val started = engine.start(
			initialState(
				first = participant(
					"intimidator", 100, skill = skill,
					abilityEffects = listOf(BattleAbilityEffect.SwitchInStatStageChange(BattleStat.ATTACK, -1)),
				),
				second = participant("holder", 50, abilityEffects = listOf(immunity)),
			),
		)
		assertEquals(0, started.participant("holder")?.statStage(BattleStat.ATTACK))

		val resolved = engine.resolveTurn(
			started,
			listOf(BattleAction.UseSkill("intimidator", skill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)
		assertEquals(0, resolved.participant("holder")?.statStage(BattleStat.DEFENSE))
		assertEquals(
			2,
			resolved.events.filterIsInstance<BattleEvent.StatStageChangeBlocked>()
				.count { it.targetActorId == "holder" && it.reason == BattleStatusBlockReason.ABILITY },
		)
	}

	@Test
	fun `switch in stat effects distinguish self from opponents`() {
		val started = BattleEngine().start(
			initialState(
				first = participant(
					"self-booster",
					100,
					abilityEffects = listOf(
						BattleAbilityEffect.SwitchInStatStageChange(BattleStat.ATTACK, 1, BattleEffectTarget.USER),
					),
				),
				second = participant(
					"evasion-lowerer",
					50,
					abilityEffects = listOf(BattleAbilityEffect.SwitchInStatStageChange(BattleStat.EVASION, -1)),
				),
			),
		)

		assertEquals(1, started.participant("self-booster")?.statStage(BattleStat.ATTACK))
		assertEquals(-1, started.participant("self-booster")?.statStage(BattleStat.EVASION))
		assertEquals(0, started.participant("evasion-lowerer")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `switch in attack reduction immunity blocks intimidate style effects`() {
		val started = BattleEngine().start(
			initialState(
				first = participant(
					"intimidator",
					100,
					abilityEffects = listOf(BattleAbilityEffect.SwitchInStatStageChange(BattleStat.ATTACK, -1)),
				),
				second = participant(
					"immune-holder",
					50,
					abilityEffects = listOf(
						BattleAbilityEffect.SwitchInStatStageReductionImmunity(setOf(BattleStat.ATTACK)),
					),
				),
			),
		)

		assertEquals(0, started.participant("immune-holder")?.statStage(BattleStat.ATTACK))
		assertEquals(
			1,
			started.events.filterIsInstance<BattleEvent.StatStageChangeBlocked>()
				.count { it.targetActorId == "immune-holder" },
		)
	}
}
