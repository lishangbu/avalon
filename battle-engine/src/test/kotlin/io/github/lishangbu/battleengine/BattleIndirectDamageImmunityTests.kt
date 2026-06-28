package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 间接伤害免疫特性的公开对照测试。
 *
 * 场景类型：特性规则 fixture。
 * 参考来源类型：成熟公开对战引擎特性实现和公开规则说明。
 * 验证重点：该特性只阻止非技能直接伤害，例如异常状态回合末伤害、天气伤害、入场陷阱伤害、
 * 技能反作用伤害和携带道具反伤；直接技能伤害、伤害倍率和其它非伤害副作用不在这里被抹掉。
 */
class BattleIndirectDamageImmunityTests {
	private val engine = BattleEngine()

	@Test
	fun `indirect damage immunity blocks residual status and sandstorm damage`() {
		val fixture = publicBattleRuleFixture(
			name = "indirect-damage-immunity-blocks-residual-status-and-weather-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Magic_Guard_(Ability)",
			),
			inputSummary = "普通属性上场成员拥有间接伤害免疫特性，并已处于灼伤状态；全场天气为沙暴。",
			expectedSummary = "完整回合末不会产生灼伤伤害或沙暴伤害事件，成员 HP 保持 100，灼伤状态仍然保留。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"protected",
					speed = 100,
					abilityId = 98,
					abilityEffects = listOf(BattleAbilityEffect.IndirectDamageImmunity),
				).copy(majorStatus = BattleMajorStatus.BURN),
				second = participant("observer", speed = 50, elementId = 6),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("indirect-damage-immunity-blocks-residual-status-and-weather-damage")
		assertEquals(100, resolved.participant("protected")?.currentHp)
		assertEquals(BattleMajorStatus.BURN, resolved.participant("protected")?.majorStatus)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.ResidualDamageApplied>().none { it.actorId == "protected" })
		assertTrue(resolved.events.filterIsInstance<BattleEvent.WeatherDamageApplied>().none { it.actorId == "protected" })
	}

	@Test
	fun `indirect damage immunity keeps direct damage boost but blocks item recoil`() {
		val fixture = publicBattleRuleFixture(
			name = "indirect-damage-immunity-keeps-direct-damage-boost-but-blocks-item-recoil",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Life_Orb",
			),
			inputSummary = "使用者拥有间接伤害免疫特性，同时携带造成伤害提升 1.3 倍并反伤最大 HP 1/10 的道具。",
			expectedSummary = "目标仍受到道具倍率提升后的直接技能伤害；使用者不会承受该道具造成的反伤。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					abilityId = 98,
					abilityEffects = listOf(BattleAbilityEffect.IndirectDamageImmunity),
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

		fixture.assertNamed("indirect-damage-immunity-keeps-direct-damage-boost-but-blocks-item-recoil")
		assertEquals(63, resolved.participant("defender")?.currentHp)
		assertEquals(100, resolved.participant("attacker")?.currentHp)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.RecoilDamageApplied>().none { it.actorId == "attacker" })
	}

	@Test
	fun `indirect damage immunity blocks move recoil and entry hazard damage`() {
		val fixture = publicBattleRuleFixture(
			name = "indirect-damage-immunity-blocks-move-recoil-and-entry-hazard-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Stealth_Rock_(move)",
			),
			inputSummary = "使用者拥有间接伤害免疫特性，使用按实际伤害 1/3 反作用的技能；另一个拥有同特性的后备成员换入隐形岩侧。",
			expectedSummary = "直接攻击正常扣减目标 HP，但不会产生技能反作用伤害；后备成员换入后不承受隐形岩入场伤害。",
		)
		val recoilSkill = damagingSkill(
			name = "反作用测试",
			hpEffects = listOf(BattleSkillHpEffect.RecoilByDamageDealt(numerator = 1, denominator = 3)),
		)
		val state = engine.start(
			initialState(
				first = participant(
					"front",
					speed = 100,
					skill = recoilSkill,
					abilityId = 98,
					abilityEffects = listOf(BattleAbilityEffect.IndirectDamageImmunity),
				),
				firstBench = listOf(
					participant(
						"reserve",
						speed = 80,
						abilityId = 98,
						abilityEffects = listOf(BattleAbilityEffect.IndirectDamageImmunity),
					),
				),
				second = participant("defender", speed = 50),
				firstSideEntryHazards = listOf(BattleSideEntryHazard(BattleSideEntryHazardKind.STEALTH_ROCK)),
			),
		)

		val afterAttack = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("front", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 0)),
		)
		val afterSwitch = engine.resolveTurn(
			afterAttack,
			listOf(BattleAction.SwitchParticipant("front", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("indirect-damage-immunity-blocks-move-recoil-and-entry-hazard-damage")
		assertEquals(76, afterAttack.participant("defender")?.currentHp)
		assertEquals(100, afterAttack.participant("front")?.currentHp)
		assertEquals(100, afterSwitch.participant("reserve")?.currentHp)
		assertTrue(afterAttack.events.filterIsInstance<BattleEvent.SkillRecoilDamageApplied>().none { it.actorId == "front" })
		assertTrue(afterSwitch.events.filterIsInstance<BattleEvent.EntryHazardDamageApplied>().none { it.actorId == "reserve" })
	}
}
