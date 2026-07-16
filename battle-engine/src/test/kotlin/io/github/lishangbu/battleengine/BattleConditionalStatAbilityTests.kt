package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleConditionalStatAbilityTests {
	@Test
	fun `status and low hp attacking stat multipliers require their conditions`() {
		val calculator = BattleDamageCalculator()
		val specialSkill = damagingSkill(damageClass = BattleDamageClass.SPECIAL)
		val burned = participant("burned", 100, skill = specialSkill).copy(
			majorStatus = BattleMajorStatus.BURN,
			abilityEffects = listOf(
				BattleAbilityEffect.AttackingStatMultiplier(
					BattleStat.SPECIAL_ATTACK,
					1.5,
					requiredMajorStatuses = setOf(BattleMajorStatus.BURN),
				),
			),
		)
		val target = participant("target", 50)
		val boosted = calculator.calculate(BattleDamageRequest(burned, target, specialSkill, neutralRules(), randomPercent = 100))
		val neutral = calculator.calculate(
			BattleDamageRequest(burned.copy(majorStatus = null), target, specialSkill, neutralRules(), randomPercent = 100),
		)
		assertTrue(boosted.amount > neutral.amount)

		val physicalSkill = damagingSkill()
		val defeatist = participant("defeatist", 100, currentHp = 50, skill = physicalSkill).copy(
			abilityEffects = listOf(BattleAbilityEffect.AttackingStatMultiplier(BattleStat.ATTACK, 0.5, maximumHpFraction = 0.5)),
		)
		val weakened = calculator.calculate(BattleDamageRequest(defeatist, target, physicalSkill, neutralRules(), randomPercent = 100))
		val healthy = calculator.calculate(
			BattleDamageRequest(defeatist.copy(currentHp = 51), target, physicalSkill, neutralRules(), randomPercent = 100),
		)
		assertTrue(weakened.amount < healthy.amount)
	}

	@Test
	fun `quick feet ignores paralysis speed reduction and applies boost`() {
		val quick = participant("quick", 80).copy(
			majorStatus = BattleMajorStatus.PARALYSIS,
			abilityEffects = listOf(BattleAbilityEffect.MajorStatusSpeedMultiplier(1.5, true)),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = quick, second = participant("target", 100))),
			listOf(BattleAction.UseSkill("quick", 1, "target"), BattleAction.UseSkill("target", 1, "quick")),
			ScriptedBattleRandom(listOf(99, 1, 15, 1, 15)),
		)

		assertEquals("quick", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
	}
}
