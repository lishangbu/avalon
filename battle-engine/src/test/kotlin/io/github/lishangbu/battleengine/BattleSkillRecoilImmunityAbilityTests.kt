package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 技能反作用伤害免疫特性的公开对照测试。
 *
 * 场景类型：特性规则 fixture。
 * 参考来源类型：成熟公开对战引擎特性实现和公开规则说明。
 * 验证重点：该特性只阻止技能自身的反作用伤害，不阻止携带道具在造成伤害后产生的固定反伤。
 */
class BattleSkillRecoilImmunityAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `skill recoil immunity blocks move recoil damage only`() {
		val fixture = publicBattleRuleFixture(
			name = "skill-recoil-immunity-blocks-move-recoil-damage",
			inputSummary = "使用者拥有技能反作用伤害免疫特性，并使用按实际伤害 1/3 反作用的物理技能。",
			expectedSummary = "目标正常受到直接技能伤害；使用者不会承受技能反作用伤害，也不会产生技能反作用伤害事件。",
		)
		val recoilSkill = damagingSkill(
			name = "反作用测试",
			hpEffects = listOf(BattleSkillHpEffect.RecoilByDamageDealt(numerator = 1, denominator = 3)),
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = recoilSkill,
					abilityEffects = listOf(BattleAbilityEffect.SkillRecoilDamageImmunity),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("skill-recoil-immunity-blocks-move-recoil-damage")
		assertEquals(72, resolved.participant("defender")?.currentHp)
		assertEquals(100, resolved.participant("attacker")?.currentHp)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.SkillRecoilDamageApplied>().none())
	}

	@Test
	fun `skill recoil immunity does not block held item recoil`() {
		val fixture = publicBattleRuleFixture(
			name = "skill-recoil-immunity-does-not-block-held-item-recoil",
			inputSummary = "使用者拥有技能反作用伤害免疫特性，同时携带造成伤害提升 1.3 倍并反伤最大 HP 1/10 的道具。",
			expectedSummary = "目标受到道具倍率提升后的直接技能伤害；使用者仍承受该道具造成的固定反伤。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					abilityEffects = listOf(BattleAbilityEffect.SkillRecoilDamageImmunity),
					itemEffects = listOf(BattleItemEffect.DamageBoostWithRecoil(multiplier = 1.3, recoilDenominator = 10)),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("skill-recoil-immunity-does-not-block-held-item-recoil")
		assertEquals(63, resolved.participant("defender")?.currentHp)
		assertEquals(90, resolved.participant("attacker")?.currentHp)
		assertEquals(10, resolved.events.filterIsInstance<BattleEvent.RecoilDamageApplied>().single().amount)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.SkillRecoilDamageApplied>().none())
	}
}
