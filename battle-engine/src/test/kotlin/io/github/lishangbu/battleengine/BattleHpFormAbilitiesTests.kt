package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleFormPair
import io.github.lishangbu.battleengine.model.BattleFormProfile
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证按 HP、等级和单向规则切换形态的特性共享同一回合末执行边界。 */
class BattleHpFormAbilitiesTests {
	@Test
	fun `zen mode enters below half hp and reverts above half hp`() {
		val standard = form(555, maxHp = 100, attack = 120)
		val zen = form(10017, maxHp = 100, attack = 30)
		val effect = BattleAbilityEffect.EndTurnHpFormChange(
			formPairs = listOf(BattleFormPair("darmanitan-standard", "darmanitan-zen")),
			thresholdNumerator = 1,
			thresholdDenominator = 2,
			alternateAtOrBelowThreshold = true,
		)
		val engine = BattleEngine()
		val holder = participant("darmanitan", 95, currentHp = 50, creatureId = 555, abilityEffects = listOf(effect),
			battleFormProfiles = mapOf("darmanitan-standard" to standard, "darmanitan-zen" to zen))
		val afterLowHp = engine.resolveTurn(
			engine.start(initialState(holder, participant("target", 40))),
			emptyList(),
			ScriptedBattleRandom(emptyList()),
		)
		val healed = afterLowHp.replaceParticipant(requireNotNull(afterLowHp.participant("darmanitan")).copy(currentHp = 60))

		val afterHealing = engine.resolveTurn(healed, emptyList(), ScriptedBattleRandom(emptyList()))

		assertEquals(zen.creatureId, afterLowHp.participant("darmanitan")?.creatureId)
		assertEquals(standard.creatureId, afterHealing.participant("darmanitan")?.creatureId)
	}

	@Test
	fun `schooling requires level twenty and more than quarter hp`() {
		val solo = form(746)
		val school = form(10127, attack = 140)
		val effect = BattleAbilityEffect.EndTurnHpFormChange(
			formPairs = listOf(BattleFormPair("wishiwashi-solo", "wishiwashi-school")),
			thresholdNumerator = 1,
			thresholdDenominator = 4,
			alternateAtOrBelowThreshold = false,
			minimumLevel = 20,
		)
		fun resolvedCreatureId(level: Int): Long? {
			val holder = participant("wishiwashi", 40, level = level, creatureId = 746, abilityEffects = listOf(effect),
				battleFormProfiles = mapOf("wishiwashi-solo" to solo, "wishiwashi-school" to school))
			return BattleEngine().resolveTurn(
				BattleEngine().start(initialState(holder, participant("target", 20))),
				emptyList(),
				ScriptedBattleRandom(emptyList()),
			).participant("wishiwashi")?.creatureId
		}

		assertEquals(solo.creatureId, resolvedCreatureId(19))
		assertEquals(school.creatureId, resolvedCreatureId(20))
	}

	@Test
	fun `shields down blocks major status only in meteor form`() {
		val meteor = form(774)
		val core = form(10136, attack = 140)
		val effect = BattleAbilityEffect.EndTurnHpFormChange(
			formPairs = listOf(BattleFormPair("minior-red-meteor", "minior-red")),
			thresholdNumerator = 1,
			thresholdDenominator = 2,
			alternateAtOrBelowThreshold = true,
			majorStatusImmuneFormCodes = setOf("minior-red-meteor"),
		)
		val burnSkill = damagingSkill(
			skillId = 1084,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(BattleMajorStatus.BURN, BattleEffectTarget.TARGET, chancePercent = 100),
			),
		)
		fun resolvedStatus(profile: BattleFormProfile, currentHp: Int): BattleMajorStatus? {
			val engine = BattleEngine()
			val target = participant(
				"minior",
				50,
				currentHp = currentHp,
				creatureId = profile.creatureId,
				abilityEffects = listOf(effect),
				battleFormProfiles = mapOf("minior-red-meteor" to meteor, "minior-red" to core),
			)
			return engine.resolveTurn(
				engine.start(initialState(participant("attacker", 100, skill = burnSkill), target)),
				listOf(BattleAction.UseSkill("attacker", burnSkill.skillId, "minior")),
				ScriptedBattleRandom(emptyList()),
			).participant("minior")?.majorStatus
		}

		assertEquals(null, resolvedStatus(meteor, currentHp = 100))
		assertEquals(BattleMajorStatus.BURN, resolvedStatus(core, currentHp = 50))
	}

	@Test
	fun `power construct changes once and adds the maximum hp difference`() {
		val base = form(10119, maxHp = 100)
		val complete = form(10120, maxHp = 150)
		val effect = BattleAbilityEffect.EndTurnHpFormChange(
			formPairs = listOf(BattleFormPair("zygarde-50-power-construct", "zygarde-complete")),
			thresholdNumerator = 1,
			thresholdDenominator = 2,
			alternateAtOrBelowThreshold = true,
			revertsWhenConditionNotMet = false,
			addsMaximumHpDifference = true,
		)
		val holder = participant("zygarde", 80, currentHp = 50, creatureId = base.creatureId,
			abilityEffects = listOf(effect),
			battleFormProfiles = mapOf("zygarde-50-power-construct" to base, "zygarde-complete" to complete))

		val resolved = BattleEngine().resolveTurn(
			BattleEngine().start(initialState(holder, participant("target", 40))),
			emptyList(),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(complete.creatureId, resolved.participant("zygarde")?.creatureId)
		assertEquals(150, resolved.participant("zygarde")?.maxHp)
		assertEquals(100, resolved.participant("zygarde")?.currentHp)
	}

	private fun form(
		creatureId: Long,
		maxHp: Int = 100,
		attack: Int = 100,
	): BattleFormProfile = BattleFormProfile(
		creatureId,
		maxHp,
		attack,
		100,
		100,
		100,
		80,
		1000,
		setOf(1),
	)
}
