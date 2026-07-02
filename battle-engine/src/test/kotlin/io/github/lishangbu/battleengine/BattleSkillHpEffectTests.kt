package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
		val rainScenario = publicBattleRuleScenario(
			name = "weather-sensitive-self-healing-skill-uses-rain-penalty-fraction",
			inputSummary = "使用者在下雨环境中使用同一个天气变量自我回复技能。",
			expectedSummary = "下雨环境命中低回复比例，技能只回复最大 HP 的 1/4。",
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
		rainScenario.assertNamed("weather-sensitive-self-healing-skill-uses-rain-penalty-fraction")
		assertEquals(86, sunResolved.participant("sun-healer")?.currentHp)
		assertEquals(66, sunResolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single().amount)
		assertEquals(45, rainResolved.participant("rain-healer")?.currentHp)
		assertEquals(25, rainResolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single().amount)
	}

	@Test
	fun `target healing status skill restores target half max hp`() {
		val scenario = publicBattleRuleScenario(
			name = "target-healing-status-skill-restores-target-half-max-hp",
			inputSummary = "使用者对当前 HP 为 35 的目标使用回复目标 1/2 最大 HP 的变化技能。",
			expectedSummary = "技能成功后目标回复 50 HP，回复事件记录被回复的目标，而不是技能使用者。",
		)
		val skill = damagingSkill(
			name = "目标回复测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			hpEffects = listOf(BattleSkillHpEffect.TargetHealMaxHpFraction(numerator = 1, denominator = 2)),
		)
		val state = engine.start(
			initialState(
				first = participant("healer", speed = 100, skill = skill),
				second = participant("target", speed = 50, currentHp = 35),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("healer", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("target-healing-status-skill-restores-target-half-max-hp")
		assertEquals(85, resolved.participant("target")?.currentHp)
		val healing = resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single()
		assertEquals("target", healing.actorId)
		assertEquals(50, healing.amount)
	}

	@Test
	fun `sandstorm sensitive self healing skill uses boost fraction`() {
		val scenario = publicBattleRuleScenario(
			name = "sandstorm-sensitive-self-healing-skill-uses-boost-fraction",
			inputSummary = "使用者在沙暴中使用默认回复 1/2、沙暴回复 2/3 最大 HP 的变化技能。",
			expectedSummary = "技能按当前沙暴天气选择 2/3 最大 HP 回复比例，回复事件记录 66 点回复。",
		)
		val skill = damagingSkill(
			name = "沙暴回复测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			hpEffects = listOf(
				BattleSkillHpEffect.SelfHealMaxHpByWeather(
					defaultFraction = BattleSkillHpEffect.HpFraction(1, 2),
					weatherFractions = mapOf(BattleWeather.SANDSTORM to BattleSkillHpEffect.HpFraction(2, 3)),
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("sand-healer", speed = 100, currentHp = 20, elementId = 6, skill = skill),
				second = participant("observer", speed = 50, elementId = 6),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("sand-healer", skillId = 1, targetActorId = "sand-healer")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("sandstorm-sensitive-self-healing-skill-uses-boost-fraction")
		assertEquals(86, resolved.participant("sand-healer")?.currentHp)
		val healing = resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single()
		assertEquals("sand-healer", healing.actorId)
		assertEquals(66, healing.amount)
	}

	@Test
	fun `terrain sensitive target healing uses grassy terrain fraction`() {
		val scenario = publicBattleRuleScenario(
			name = "terrain-sensitive-target-healing-uses-grassy-terrain-fraction",
			inputSummary = "使用者在青草场地对当前 HP 为 20 的目标使用场地变量目标治疗技能。",
			expectedSummary = "青草场地使命中目标先按最大 HP 的 2/3 回复；回合结束时青草场地自身再执行一次 1/16 回复。",
		)
		val skill = damagingSkill(
			name = "场地目标回复测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			hpEffects = listOf(
				BattleSkillHpEffect.TargetHealMaxHpByTerrain(
					defaultFraction = BattleSkillHpEffect.HpFraction(1, 2),
					terrainFractions = mapOf(BattleTerrain.GRASSY to BattleSkillHpEffect.HpFraction(2, 3)),
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("healer", speed = 100, skill = skill),
				second = participant("target", speed = 50, currentHp = 20),
				environment = BattleEnvironment(terrain = BattleTerrain.GRASSY),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("healer", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("terrain-sensitive-target-healing-uses-grassy-terrain-fraction")
		assertEquals(92, resolved.participant("target")?.currentHp)
		val healing = resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single()
		assertEquals("target", healing.actorId)
		assertEquals(66, healing.amount)
	}

	@Test
	fun `strength sap heals by target current attack before lowering attack`() {
		val scenario = publicBattleRuleScenario(
			name = "strength-sap-heals-by-target-current-attack-before-lowering-attack",
			inputSummary = "使用者对攻击基础值 80、攻击阶级 -1 的目标使用按目标当前攻击回复的变化技能。",
			expectedSummary = "技能先按目标降攻前的当前攻击实数 53 回复使用者，然后才把目标攻击从 -1 降到 -2。",
		)
		val skill = strengthSapLikeSkill()
		val state = engine.start(
			initialState(
				first = participant("sap-user", speed = 100, currentHp = 20, skill = skill),
				second = participant("target", speed = 50).copy(
					attack = 80,
					statStages = mapOf(BattleStat.ATTACK to -1),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("sap-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1)),
		)

		scenario.assertNamed("strength-sap-heals-by-target-current-attack-before-lowering-attack")
		assertEquals(73, resolved.participant("sap-user")?.currentHp)
		assertEquals(-2, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		val healing = resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single()
		val statChange = resolved.events.filterIsInstance<BattleEvent.StatStageChanged>().single()
		assertEquals(53, healing.amount)
		assertEquals(-1, statChange.delta)
		assertEquals(-2, statChange.currentStage)
		assertTrue(
			resolved.events.indexOf(healing) < resolved.events.indexOf(statChange),
			"按目标攻击回复必须先于本技能造成的攻击阶级下降，避免读取到降攻后的数值",
		)
	}

	@Test
	fun `strength sap fails when target attack stage is already minimum`() {
		val scenario = publicBattleRuleScenario(
			name = "strength-sap-fails-when-target-attack-stage-is-already-minimum",
			inputSummary = "使用者对攻击阶级已经 -6 的目标使用按目标当前攻击回复的变化技能。",
			expectedSummary = "技能在命中前以稳定失败原因结束，不回复 HP，也不追加能力阶级变化事件。",
		)
		val skill = strengthSapLikeSkill()
		val state = engine.start(
			initialState(
				first = participant("sap-user", speed = 100, currentHp = 20, skill = skill),
				second = participant("target", speed = 50).copy(
					statStages = mapOf(BattleStat.ATTACK to -6),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("sap-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("strength-sap-fails-when-target-attack-stage-is-already-minimum")
		assertEquals(20, resolved.participant("sap-user")?.currentHp)
		assertEquals(-6, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals("target-attack-stage-minimum", resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageChanged>())
	}

	@Test
	fun `strength sap healing succeeds even when substitute blocks attack drop`() {
		val scenario = publicBattleRuleScenario(
			name = "strength-sap-healing-succeeds-even-when-substitute-blocks-attack-drop",
			inputSummary = "目标带有替身，使用者对其使用按目标当前攻击回复并尝试降攻的变化技能。",
			expectedSummary = "替身阻止来自对手的攻击阶级下降，但不阻止已经按目标当前攻击实数结算的回复。",
		)
		val skill = strengthSapLikeSkill()
		val state = engine.start(
			initialState(
				first = participant("sap-user", speed = 100, currentHp = 20, skill = skill),
				second = participant("target", speed = 50).copy(
					attack = 60,
					statStages = mapOf(BattleStat.ATTACK to -1),
					substituteHp = 25,
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("sap-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1)),
		)

		scenario.assertNamed("strength-sap-healing-succeeds-even-when-substitute-blocks-attack-drop")
		assertEquals(60, resolved.participant("sap-user")?.currentHp)
		assertEquals(-1, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		val healing = resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single()
		assertEquals(40, healing.amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageChanged>())
	}

	@Test
	fun `purify cures target major status before healing user`() {
		val scenario = publicBattleRuleScenario(
			name = "purify-cures-target-major-status-before-healing-user",
			inputSummary = "使用者未满 HP，目标处于灼伤，使用治愈目标主要异常后自我回复的变化技能。",
			expectedSummary = "技能先清除目标灼伤并记录状态解除事件，然后回复使用者最大 HP 的一半。",
		)
		val skill = purifyLikeSkill()
		val state = engine.start(
			initialState(
				first = participant("purify-user", speed = 100, currentHp = 20, skill = skill),
				second = participant("target", speed = 50).copy(majorStatus = BattleMajorStatus.BURN),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("purify-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("purify-cures-target-major-status-before-healing-user")
		assertEquals(null, resolved.participant("target")?.majorStatus)
		assertEquals(70, resolved.participant("purify-user")?.currentHp)
		val cleared = resolved.events.filterIsInstance<BattleEvent.StatusCleared>().single()
		val healing = resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single()
		assertEquals("target", cleared.actorId)
		assertEquals(BattleMajorStatus.BURN, cleared.status)
		assertEquals("purify-user", healing.actorId)
		assertEquals(50, healing.amount)
		assertTrue(
			resolved.events.indexOf(cleared) < resolved.events.indexOf(healing),
			"治愈目标主要异常必须先于使用者回复，确保 replay 顺序和规则描述一致",
		)
	}

	@Test
	fun `purify fails when target has no major status`() {
		val scenario = publicBattleRuleScenario(
			name = "purify-fails-when-target-has-no-major-status",
			inputSummary = "目标没有主要异常，使用者尝试使用治愈目标异常后自我回复的变化技能。",
			expectedSummary = "技能以目标没有主要异常的稳定原因失败，不清状态也不回复 HP。",
		)
		val skill = purifyLikeSkill()
		val state = engine.start(
			initialState(
				first = participant("purify-user", speed = 100, currentHp = 20, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("purify-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("purify-fails-when-target-has-no-major-status")
		assertEquals(20, resolved.participant("purify-user")?.currentHp)
		assertEquals("target-has-no-major-status", resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusCleared>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>())
	}

	@Test
	fun `purify still cures target major status when user hp is full`() {
		val scenario = publicBattleRuleScenario(
			name = "purify-still-cures-target-major-status-when-user-hp-is-full",
			inputSummary = "使用者 HP 已满，目标处于麻痹，使用治愈目标主要异常后自我回复的变化技能。",
			expectedSummary = "技能仍然清除目标麻痹；因为使用者没有缺失 HP，所以不会产生技能回复事件。",
		)
		val skill = purifyLikeSkill()
		val state = engine.start(
			initialState(
				first = participant("purify-user", speed = 100, currentHp = 100, skill = skill),
				second = participant("target", speed = 50).copy(majorStatus = BattleMajorStatus.PARALYSIS),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("purify-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("purify-still-cures-target-major-status-when-user-hp-is-full")
		assertEquals(null, resolved.participant("target")?.majorStatus)
		assertEquals(100, resolved.participant("purify-user")?.currentHp)
		val cleared = resolved.events.filterIsInstance<BattleEvent.StatusCleared>().single()
		assertEquals("target", cleared.actorId)
		assertEquals(BattleMajorStatus.PARALYSIS, cleared.status)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>())
	}

	private fun strengthSapLikeSkill() =
		damagingSkill(
			name = "吸取力量测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = 100,
			hpEffects = listOf(BattleSkillHpEffect.SelfHealByTargetCurrentAttack),
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.ATTACK,
					target = BattleEffectTarget.TARGET,
					stageDelta = -1,
					chancePercent = 100,
				),
			),
		)

	private fun purifyLikeSkill() =
		damagingSkill(
			name = "净化测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			hpEffects = listOf(
				BattleSkillHpEffect.SelfHealAfterTargetMajorStatusCure(
					numerator = 1,
					denominator = 2,
				),
			),
		)
}
