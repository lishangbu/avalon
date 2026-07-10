package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证攻击方特性在本次技能中无视目标侧防守特性的现代规则。
 *
 * 场景类型：技能结算中的目标侧特性抑制 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。公开实现会在技能修改阶段给本次技能打上
 * `ignoreAbility` 标记；本地引擎把该事实建模为结构化特性效果，并只在攻击方对对手目标使用技能时跳过目标
 * 侧防守特性。该规则不抑制目标携带道具、属性天然免疫、场地免疫、使用者自己的攻击侧特性或非技能伤害来源。
 */
class BattleTargetAbilityIgnoreTests {
	private val engine = BattleEngine()

	@Test
	fun `target ability ignore bypasses element absorb ability`() {
		val scenario = publicBattleRuleScenario(
			name = "target-ability-ignore-bypasses-element-absorb",
			inputSummary = "攻击方拥有无视目标特性效果，使用匹配目标吸收特性的属性攻击。",
			expectedSummary = "目标的属性吸收特性不触发，技能进入普通伤害结算并造成直接伤害。",
		)
		val skill = damagingSkill(name = "无视吸收测试", elementId = 13)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = skill,
					abilityId = 104,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreTargetAbilityEffects()),
				),
				second = participant(
					"absorber",
					speed = 80,
					currentHp = 50,
					abilityId = 10,
					abilityEffects = listOf(BattleAbilityEffect.ElementSkillAbsorbHeal(elementId = 13)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "absorber")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		scenario.assertNamed("target-ability-ignore-bypasses-element-absorb")
		assertEquals(31, resolved.participant("absorber")?.currentHp)
		assertEquals(19, damage.amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillAbsorbedByAbility>())
	}

	@Test
	fun `target ability ignore bypasses full hp survival ability`() {
		val scenario = publicBattleRuleScenario(
			name = "target-ability-ignore-bypasses-full-hp-survival",
			inputSummary = "攻击方拥有无视目标特性效果，目标满 HP 且拥有满 HP 致命伤害保留特性。",
			expectedSummary = "目标特性不触发，致命直接伤害把目标 HP 扣到 0，不产生保留 1 HP 事件。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = lethalSkill(),
					abilityId = 104,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreTargetAbilityEffects()),
				),
				second = participant(
					"survivor",
					speed = 50,
					abilityId = 5,
					abilityEffects = listOf(BattleAbilityEffect.SurviveFatalDamageAtFullHp()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 9001, targetActorId = "survivor")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("target-ability-ignore-bypasses-full-hp-survival")
		assertEquals(0, resolved.participant("survivor")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.FatalDamageSurvived>())
		assertEquals(100, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	@Test
	fun `target ability ignore bypasses defender damage stat stage ignore`() {
		val scenario = publicBattleRuleScenario(
			name = "target-ability-ignore-bypasses-defender-stat-stage-ignore",
			inputSummary = "攻击方攻击阶级为 +2 且拥有无视目标特性效果，防守方拥有无视对手伤害阶级变化特性。",
			expectedSummary = "防守方特性不参与本次公式，物理伤害读取攻击方 +2 攻击阶级。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					abilityId = 104,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreTargetAbilityEffects()),
				).copy(statStages = mapOf(BattleStat.ATTACK to 2)),
				second = participant(
					"defender",
					speed = 50,
					abilityId = 109,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreOpponentDamageStatStages()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("target-ability-ignore-bypasses-defender-stat-stage-ignore")
		assertEquals(45, resolved.participant("defender")?.currentHp)
		assertEquals(55, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	@Test
	fun `target ability ignore bypasses target major status immunity`() {
		val scenario = publicBattleRuleScenario(
			name = "target-ability-ignore-bypasses-target-status-immunity",
			inputSummary = "攻击方拥有无视目标特性效果，使用附加麻痹的变化技能命中拥有麻痹免疫特性的目标。",
			expectedSummary = "目标特性免疫被跳过，麻痹状态成功写入；属性、场地和道具免疫不在该 scenario 范围内。",
		)
		val skill = damagingSkill(
			name = "无视状态免疫测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.PARALYSIS,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = skill,
					abilityId = 104,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreTargetAbilityEffects()),
				),
				second = participant(
					"target",
					speed = 80,
					abilityId = 7,
					abilityEffects = listOf(
						BattleAbilityEffect.MajorStatusImmunity(setOf(BattleMajorStatus.PARALYSIS)),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("target-ability-ignore-bypasses-target-status-immunity")
		assertEquals(BattleMajorStatus.PARALYSIS, resolved.participant("target")?.majorStatus)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>())
		assertEquals(BattleMajorStatus.PARALYSIS, resolved.events.filterIsInstance<BattleEvent.StatusApplied>().single().status)
	}

	private fun lethalSkill() =
		damagingSkill(
			skillId = 9001,
			name = "无视保命测试",
			power = 250,
		)
}
