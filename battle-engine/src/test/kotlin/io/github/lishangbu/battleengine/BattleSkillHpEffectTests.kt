package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证技能自身带来的 HP 回复效果。
 *
 * 场景类型：技能 HP 效果 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。吸取类技能按本次造成的实际伤害回复使用者；自我回复类变化
 * 技能按使用者最大 HP 的固定比例回复；反作用伤害按目标实际损失 HP 计算。
 * 验证重点：回复和反作用来源以专用技能事件表达，并且最终数值会按当前缺失 HP 或剩余 HP 夹取。
 */
class BattleSkillHpEffectTests {
	private val engine = BattleEngine()

	@Test
	fun `draining damage skill heals user by half damage dealt`() {
		val scenario = publicBattleRuleScenario(
			name = "draining-damage-skill-heals-user-by-half-damage-dealt",
			inputSummary = "使用者未满 HP，使用带有吸取回复效果的伤害技能命中目标。",
			expectedSummary = "目标受到普通伤害后，使用者按本次实际伤害的 1/2 回复 HP。",
		)
		val skill = damagingSkill(
			name = "吸取测试",
			hpEffects = listOf(BattleSkillHpEffect.DrainDamage(numerator = 1, denominator = 2)),
		)
		val state = engine.start(
			initialState(
				first = participant("drain-user", speed = 100, currentHp = 70, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("drain-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("draining-damage-skill-heals-user-by-half-damage-dealt")
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount
		val healing = resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single()
		assertEquals(damage / 2, healing.amount)
		assertEquals("drain-user", healing.actorId)
		assertEquals(1, healing.skillId)
		assertEquals(70 + damage / 2, resolved.participant("drain-user")?.currentHp)
	}

	@Test
	fun `draining damage skill honors configured drain fraction`() {
		val scenario = publicBattleRuleScenario(
			name = "draining-damage-skill-honors-configured-drain-fraction",
			inputSummary = "使用者未满 HP，使用带有 3/4 吸取比例的伤害技能命中目标。",
			expectedSummary = "目标受到普通伤害后，使用者按本次实际伤害的 3/4 回复 HP。",
		)
		val skill = damagingSkill(
			name = "高比例吸取测试",
			hpEffects = listOf(BattleSkillHpEffect.DrainDamage(numerator = 3, denominator = 4)),
		)
		val state = engine.start(
			initialState(
				first = participant("drain-user", speed = 100, currentHp = 20, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("drain-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("draining-damage-skill-honors-configured-drain-fraction")
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount
		val expectedHealing = (damage * 3) / 4
		val healing = resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single()
		assertEquals(expectedHealing, healing.amount)
		assertEquals(20 + expectedHealing, resolved.participant("drain-user")?.currentHp)
	}

	@Test
	fun `recoil damage skill damages user by rounded fraction of hp actually lost by target`() {
		val scenario = publicBattleRuleScenario(
			name = "recoil-damage-skill-uses-target-hp-actually-lost",
			inputSummary = "目标剩余 HP 低于公式伤害时，使用者使用带 1/3 反作用伤害的物理技能命中目标。",
			expectedSummary = "目标只损失剩余 17 HP；反作用伤害按 17 的 1/3 四舍五入为 6，而不是按溢出公式伤害计算。",
		)
		val skill = damagingSkill(
			name = "反作用测试",
			hpEffects = listOf(BattleSkillHpEffect.RecoilByDamageDealt(numerator = 1, denominator = 3)),
		)
		val state = engine.start(
			initialState(
				first = participant("recoil-user", speed = 100, currentHp = 100, skill = skill),
				second = participant("low-hp-target", speed = 50, currentHp = 17),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("recoil-user", skillId = 1, targetActorId = "low-hp-target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("recoil-damage-skill-uses-target-hp-actually-lost")
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		val recoil = resolved.events.filterIsInstance<BattleEvent.SkillRecoilDamageApplied>().single()
		assertEquals(17, damage.amount)
		assertEquals(6, recoil.amount)
		assertEquals(17, recoil.sourceDamageAmount)
		assertEquals(94, resolved.participant("recoil-user")?.currentHp)
		assertEquals(0, resolved.participant("low-hp-target")?.currentHp)
	}

	@Test
	fun `self healing status skill restores half max hp`() {
		val scenario = publicBattleRuleScenario(
			name = "self-healing-status-skill-restores-half-max-hp",
			inputSummary = "使用者未满 HP，使用固定回复 1/2 最大 HP 的变化技能。",
			expectedSummary = "技能成功后使用者回复最大 HP 的 1/2，并产生技能回复事件。",
		)
		val skill = damagingSkill(
			name = "自我回复测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			hpEffects = listOf(BattleSkillHpEffect.SelfHealMaxHpFraction(numerator = 1, denominator = 2)),
		)
		val state = engine.start(
			initialState(
				first = participant("heal-user", speed = 100, currentHp = 40, skill = skill),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("heal-user", skillId = 1, targetActorId = "heal-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("self-healing-status-skill-restores-half-max-hp")
		assertEquals(90, resolved.participant("heal-user")?.currentHp)
		val healing = resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single()
		assertEquals("heal-user", healing.actorId)
		assertEquals(1, healing.skillId)
		assertEquals(50, healing.amount)
	}

	@Test
	fun `weather sensitive self healing skill uses current weather fraction`() {
		val scenario = publicBattleRuleScenario(
			name = "weather-sensitive-self-healing-skill-uses-current-weather-fraction",
			inputSummary = "使用者分别在晴天和下雨环境中使用天气变量自我回复技能。",
			expectedSummary = "晴天回复最大 HP 的 2/3；下雨回复最大 HP 的 1/4。",
		)
		val skill = damagingSkill(
			name = "天气回复测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			hpEffects = listOf(
				BattleSkillHpEffect.SelfHealMaxHpByWeather(
					defaultFraction = BattleSkillHpEffect.HpFraction(1, 2),
					weatherFractions = mapOf(
						BattleWeather.SUN to BattleSkillHpEffect.HpFraction(2, 3),
						BattleWeather.RAIN to BattleSkillHpEffect.HpFraction(1, 4),
						BattleWeather.SANDSTORM to BattleSkillHpEffect.HpFraction(1, 4),
						BattleWeather.SNOW to BattleSkillHpEffect.HpFraction(1, 4),
					),
				),
			),
		)
		val sunState = engine.start(
			initialState(
				first = participant("sun-healer", speed = 100, currentHp = 20, skill = skill),
				second = participant("sun-observer", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.SUN),
			),
		)
		val rainState = engine.start(
			initialState(
				first = participant("rain-healer", speed = 100, currentHp = 20, skill = skill),
				second = participant("rain-observer", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.RAIN),
			),
		)

		val sunResolved = engine.resolveTurn(
			sunState,
			listOf(BattleAction.UseSkill("sun-healer", skillId = 1, targetActorId = "sun-healer")),
			ScriptedBattleRandom(emptyList()),
		)
		val rainResolved = engine.resolveTurn(
			rainState,
			listOf(BattleAction.UseSkill("rain-healer", skillId = 1, targetActorId = "rain-healer")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("weather-sensitive-self-healing-skill-uses-current-weather-fraction")
		assertEquals(86, sunResolved.participant("sun-healer")?.currentHp)
		assertEquals(66, sunResolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single().amount)
		assertEquals(45, rainResolved.participant("rain-healer")?.currentHp)
		assertEquals(25, rainResolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single().amount)
	}
}
