package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleFaintAbilityTests {
	@Test
	fun `battle bond raises attack special attack and speed only once per battle`() {
		val holder = participant(
			"holder",
			100,
			abilityEffects = listOf(
				BattleAbilityEffect.OncePerBattleCausedFaintMultiStatBoost(
					setOf(BattleStat.ATTACK, BattleStat.SPECIAL_ATTACK, BattleStat.SPEED),
					1,
				),
			),
		)
		val target = participant("target", 50).copy(currentHp = 0)
		val started = BattleEngine().start(initialState(first = holder, second = target.copy(currentHp = 1)))
		val afterFirst = started.replaceParticipant(target)
			.handleFaintsAndResult(listOf(target), killerActorId = "holder")
		val afterSecond = afterFirst.handleFaintsAndResult(listOf(target), killerActorId = "holder")

		assertEquals(1, afterSecond.participant("holder")?.statStage(BattleStat.ATTACK))
		assertEquals(1, afterSecond.participant("holder")?.statStage(BattleStat.SPECIAL_ATTACK))
		assertEquals(1, afterSecond.participant("holder")?.statStage(BattleStat.SPEED))
		assertEquals(true, afterSecond.participant("holder")?.oncePerBattleFaintBoostActivated)
	}

	@Test
	fun `holder gains attack after causing opponent to faint`() {
		val skill = fatalSkill()
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant(
							"holder",
							100,
							skill = skill,
							abilityEffects = listOf(
								BattleAbilityEffect.FaintStatStageBoost(BattleStat.ATTACK, 1, true),
							),
						),
						second = participant("target", 50),
					),
				),
				listOf(BattleAction.UseSkill("holder", skill.skillId, "target")),
				ScriptedBattleRandom(emptyList()),
			)
		}

		assertEquals(1, resolved.participant("holder")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `holder gains special attack after causing opponent to faint`() {
		val skill = fatalSkill()
		val resolved = resolveFatalSkill(
			participant(
				"holder",
				100,
				skill = skill,
				abilityEffects = listOf(
					BattleAbilityEffect.FaintStatStageBoost(BattleStat.SPECIAL_ATTACK, 1, true),
				),
			),
			skill,
		)

		assertEquals(1, resolved.participant("holder")?.statStage(BattleStat.SPECIAL_ATTACK))
	}

	@Test
	fun `soul heart gains special attack when any participant faints`() {
		val engine = BattleEngine()
		val holder = participant(
			"holder",
			100,
			abilityEffects = listOf(
				BattleAbilityEffect.FaintStatStageBoost(BattleStat.SPECIAL_ATTACK, 1, false),
			),
		)
		val target = participant("target", 50)
		val started = engine.start(initialState(first = holder, second = target))
		val faintedTarget = target.copy(currentHp = 0)
		val resolved = started
			.replaceParticipant(faintedTarget)
			.handleFaintsAndResult(listOf(faintedTarget), killerActorId = null)

		assertEquals(1, resolved.participant("holder")?.statStage(BattleStat.SPECIAL_ATTACK))
	}

	@Test
	fun `beast boost chooses the highest raw stat using stable tie order`() {
		val skill = fatalSkill()
		val holder = participant(
			"holder",
			100,
			skill = skill,
			abilityEffects = listOf(BattleAbilityEffect.FaintHighestStatBoost()),
		).copy(
			attack = 120,
			defense = 120,
			specialAttack = 110,
			specialDefense = 90,
			speed = 80,
		)
		val resolved = resolveFatalSkill(holder, skill)

		assertEquals(1, resolved.participant("holder")?.statStage(BattleStat.ATTACK))
		assertEquals(0, resolved.participant("holder")?.statStage(BattleStat.DEFENSE))
	}

	@Test
	fun `damage that does not faint and a different killer do not trigger caused faint boost`() {
		val weakSkill = damagingSkill(power = null, fixedDamage = BattleFixedDamage.FixedAmount(1))
		val engine = BattleEngine()
		val holder = participant(
			"holder",
			100,
			skill = weakSkill,
			abilityEffects = listOf(
				BattleAbilityEffect.FaintStatStageBoost(BattleStat.ATTACK, 1, true),
			),
		)
		val target = participant("target", 50)
		val afterDamage = engine.resolveTurn(
			engine.start(initialState(first = holder, second = target)),
			listOf(BattleAction.UseSkill("holder", weakSkill.skillId, "target")),
			ScriptedBattleRandom(emptyList()),
		)
		val faintedTarget = afterDamage.participant("target")!!.copy(currentHp = 0)
		val afterDifferentKiller = afterDamage
			.replaceParticipant(faintedTarget)
			.handleFaintsAndResult(listOf(faintedTarget), killerActorId = "someone-else")

		assertEquals(0, afterDamage.participant("holder")?.statStage(BattleStat.ATTACK))
		assertEquals(0, afterDifferentKiller.participant("holder")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `multiple faints in one stage boost once per faint and clamp at six`() {
		val holder = participant(
			"holder",
			100,
			abilityEffects = listOf(
				BattleAbilityEffect.FaintStatStageBoost(BattleStat.SPECIAL_ATTACK, 4, false),
			),
		)
		val ally = participant("ally", 90)
		val target = participant("target", 80)
		val otherTarget = participant("other-target", 70)
		val started = BattleEngine().start(doubleInitialState(holder, ally, target, otherTarget))
		val faintedTarget = target.copy(currentHp = 0)
		val faintedOtherTarget = otherTarget.copy(currentHp = 0)
		val resolved = started
			.replaceParticipant(faintedTarget)
			.replaceParticipant(faintedOtherTarget)
			.handleFaintsAndResult(listOf(faintedTarget, faintedOtherTarget), killerActorId = null)

		assertEquals(6, resolved.participant("holder")?.statStage(BattleStat.SPECIAL_ATTACK))
	}

	private fun fatalSkill() = damagingSkill(
		skillId = 901,
		power = null,
		fixedDamage = BattleFixedDamage.FixedAmount(200),
	)

	private fun resolveFatalSkill(
		holder: io.github.lishangbu.battleengine.model.BattleParticipant,
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
	) = BattleEngine().let { engine ->
		engine.resolveTurn(
			engine.start(initialState(first = holder, second = participant("target", 50))),
			listOf(BattleAction.UseSkill(holder.actorId, skill.skillId, "target")),
			ScriptedBattleRandom(emptyList()),
		)
	}
}
