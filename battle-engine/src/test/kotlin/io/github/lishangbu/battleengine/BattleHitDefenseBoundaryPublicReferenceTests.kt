package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证现代主系列命中、防护、替身和免疫边界。
 *
 * 场景类型：命中随机消费、保护屏障、替身失败/阻挡/例外、属性和标签免疫 场景。
 * 参考来源类型：公开成熟对战引擎的行动结算实现、公开技能资料，以及公开命中、保护、替身和属性免疫说明。
 * 这些规则都发生在“技能已经排队”和“实际造成伤害或附加效果”之间，顺序稍有偏差就会让 PP、随机轨迹、
 * replay 和后续追加效果全部偏移。
 * 验证重点：必中和修正后必中的技能不消费命中随机；保护位于命中之前；替身只阻止对手非声音类技能；
 * 属性或标签免疫会在要害、伤害随机和附加效果前短路。
 */
class BattleHitDefenseBoundaryPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `sure hit skill skips accuracy random and proceeds to damage`() {
		val scenario = publicBattleRuleScenario(
			name = "sure-hit-skill-skips-accuracy-random-and-proceeds-to-damage",
			inputSummary = "技能命中字段为空，表示当前规则下必中。",
			expectedSummary = "技能不消费命中随机，直接进入要害和伤害随机流程并造成伤害。",
		)
		val random = ScriptedBattleRandom(listOf(1, 15))
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = participant("attacker", speed = 100), second = participant("target", speed = 50))),
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("sure-hit-skill-skips-accuracy-random-and-proceeds-to-damage")
		assertEquals(72, resolved.participant("target")?.currentHp)
		assertEquals(listOf("critical hit for 1", "damage random for 1"), random.consumedReasons())
	}

	@Test
	fun `accuracy roll equal to modified accuracy still hits`() {
		val scenario = publicBattleRuleScenario(
			name = "accuracy-roll-equal-to-modified-accuracy-still-hits",
			inputSummary = "基础命中 50 的技能掷出命中随机 50。",
			expectedSummary = "命中判定使用小于等于阈值，掷点等于有效命中率时仍命中。",
		)
		val skill = damagingSkill(skillId = 11, accuracy = 50)
		val random = ScriptedBattleRandom(listOf(49, 1, 15))
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = participant("attacker", speed = 100, skill = skill), second = participant("target", speed = 50))),
			listOf(BattleAction.UseSkill("attacker", skillId = 11, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("accuracy-roll-equal-to-modified-accuracy-still-hits")
		assertEquals(72, resolved.participant("target")?.currentHp)
		assertEquals(listOf("accuracy for 11", "critical hit for 11", "damage random for 11"), random.consumedReasons())
	}

	@Test
	fun `accuracy miss skips critical hit and damage random`() {
		val scenario = publicBattleRuleScenario(
			name = "accuracy-miss-skips-critical-hit-and-damage-random",
			inputSummary = "基础命中 50 的技能掷出命中随机 51。",
			expectedSummary = "技能未命中后不再消费要害或伤害随机数，也不造成伤害。",
		)
		val skill = damagingSkill(skillId = 12, accuracy = 50)
		val random = ScriptedBattleRandom(listOf(50))
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = participant("attacker", speed = 100, skill = skill), second = participant("target", speed = 50))),
			listOf(BattleAction.UseSkill("attacker", skillId = 12, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("accuracy-miss-skips-critical-hit-and-damage-random")
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals(51, resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single().accuracyRoll)
		assertEquals(listOf("accuracy for 12"), random.consumedReasons())
	}

	@Test
	fun `positive accuracy stage can make a seventy five accuracy skill sure hit`() {
		val scenario = publicBattleRuleScenario(
			name = "positive-accuracy-stage-can-make-seventy-five-accuracy-skill-sure-hit",
			inputSummary = "使用者命中 +1，技能基础命中 75。",
			expectedSummary = "修正后有效命中率达到 100，不消费命中随机并继续造成伤害。",
		)
		val skill = damagingSkill(skillId = 13, accuracy = 75)
		val attacker = participant("attacker", speed = 100, skill = skill).copy(statStages = mapOf(BattleStat.ACCURACY to 1))
		val random = ScriptedBattleRandom(listOf(1, 15))
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = attacker, second = participant("target", speed = 50))),
			listOf(BattleAction.UseSkill("attacker", skillId = 13, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("positive-accuracy-stage-can-make-seventy-five-accuracy-skill-sure-hit")
		assertEquals(72, resolved.participant("target")?.currentHp)
		assertEquals(listOf("critical hit for 13", "damage random for 13"), random.consumedReasons())
	}

	@Test
	fun `positive evasion stage can make a one hundred accuracy skill miss`() {
		val scenario = publicBattleRuleScenario(
			name = "positive-evasion-stage-can-make-one-hundred-accuracy-skill-miss",
			inputSummary = "目标闪避 +1，攻击方使用基础命中 100 的技能，命中随机掷出 76。",
			expectedSummary = "有效命中率降为 75，掷点 76 未命中且不继续结算伤害。",
		)
		val skill = damagingSkill(skillId = 14, accuracy = 100)
		val target = participant("target", speed = 50).copy(statStages = mapOf(BattleStat.EVASION to 1))
		val random = ScriptedBattleRandom(listOf(75))
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = participant("attacker", speed = 100, skill = skill), second = target)),
			listOf(BattleAction.UseSkill("attacker", skillId = 14, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("positive-evasion-stage-can-make-one-hundred-accuracy-skill-miss")
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals(76, resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single().accuracyRoll)
		assertEquals(listOf("accuracy for 14"), random.consumedReasons())
	}

	@Test
	fun `weather sure hit override skips accuracy random`() {
		val scenario = publicBattleRuleScenario(
			name = "weather-sure-hit-override-skips-accuracy-random",
			inputSummary = "当前天气把基础命中 50 的技能覆盖为必中。",
			expectedSummary = "天气覆盖后的命中率为空，不消费命中随机并继续造成伤害。",
		)
		val skill = damagingSkill(
			skillId = 15,
			accuracy = 50,
			accuracyOverridesByWeather = mapOf(BattleWeather.RAIN to null),
		)
		val random = ScriptedBattleRandom(listOf(1, 15))
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", speed = 100, skill = skill),
					second = participant("target", speed = 50),
					environment = BattleEnvironment(weather = BattleWeather.RAIN),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skillId = 15, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("weather-sure-hit-override-skips-accuracy-random")
		assertEquals(72, resolved.participant("target")?.currentHp)
		assertEquals(listOf("critical hit for 15", "damage random for 15"), random.consumedReasons())
	}

	@Test
	fun `first successful protection consumes no chance random`() {
		val scenario = publicBattleRuleScenario(
			name = "first-successful-protection-consumes-no-chance-random",
			inputSummary = "成员首次连续保护计数为 0 时使用保护类技能。",
			expectedSummary = "首次保护必定成功，不消费保护概率随机，并把连续保护计数记为 1。",
		)
		val random = ScriptedBattleRandom(emptyList())
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = participant("protector", speed = 100, skill = protectionSkill()), second = participant("target", speed = 50))),
			listOf(BattleAction.UseSkill("protector", skillId = 2, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("first-successful-protection-consumes-no-chance-random")
		assertEquals(1, resolved.participant("protector")?.protectionChain)
		assertEquals(9, resolved.participant("protector")?.skillSlot(2)?.remainingPp)
		assertEquals(emptyList(), random.consumedReasons())
		assertEquals("protector", resolved.events.filterIsInstance<BattleEvent.ProtectionStarted>().single().actorId)
	}

	@Test
	fun `second consecutive protection success consumes one third chance random`() {
		val scenario = publicBattleRuleScenario(
			name = "second-consecutive-protection-success-consumes-one-third-chance-random",
			inputSummary = "成员已有一次连续保护成功计数，第二次使用保护类技能并掷出成功值。",
			expectedSummary = "第二次保护消费 1/3 概率随机，掷到 0 成功并把连续保护计数推进到 2。",
		)
		val protector = participant("protector", speed = 100, skill = protectionSkill()).copy(protectionChain = 1)
		val random = ScriptedBattleRandom(listOf(0))
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = protector, second = participant("target", speed = 50))),
			listOf(BattleAction.UseSkill("protector", skillId = 2, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("second-consecutive-protection-success-consumes-one-third-chance-random")
		assertEquals(2, resolved.participant("protector")?.protectionChain)
		assertEquals(listOf("protection chance for 2"), random.consumedReasons())
	}

	@Test
	fun `failed consecutive protection leaves user unprotected for later damage`() {
		val scenario = publicBattleRuleScenario(
			name = "failed-consecutive-protection-leaves-user-unprotected-for-later-damage",
			inputSummary = "成员第二次连续保护掷点失败，较慢对手随后攻击该成员。",
			expectedSummary = "保护失败后不建立屏障，后续攻击不会被阻挡并正常造成伤害。",
		)
		val protector = participant("protector", speed = 100, skill = protectionSkill()).copy(protectionChain = 1)
		val random = ScriptedBattleRandom(listOf(1, 1, 15))
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = protector, second = participant("attacker", speed = 50))),
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "protector"),
			),
			random,
		)

		scenario.assertNamed("failed-consecutive-protection-leaves-user-unprotected-for-later-damage")
		assertEquals(72, resolved.participant("protector")?.currentHp)
		assertEquals(0, resolved.participant("protector")?.protectionChain)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertEquals(listOf("protection chance for 2", "critical hit for 1", "damage random for 1"), random.consumedReasons())
	}

	@Test
	fun `protection blocks ally affected skill in double battle`() {
		val scenario = publicBattleRuleScenario(
			name = "protection-blocks-ally-affected-skill-in-double-battle",
			inputSummary = "双打中同侧伙伴对已经建立保护的成员使用受保护影响的技能。",
			expectedSummary = "保护屏障不区分攻击来源，同侧伙伴的受保护影响技能也被阻挡。",
		)
		val resolved = engine.resolveTurn(
			engine.start(
				doubleInitialState(
					firstA = participant("protector", speed = 100, skill = protectionSkill()),
					firstB = participant("ally", speed = 50),
					secondA = participant("opponent-left", speed = 80),
					secondB = participant("opponent-right", speed = 70),
				),
			),
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "ally"),
				BattleAction.UseSkill("ally", skillId = 1, targetActorId = "protector"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("protection-blocks-ally-affected-skill-in-double-battle")
		assertEquals(100, resolved.participant("protector")?.currentHp)
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single()
		assertEquals("ally", blocked.actorId)
		assertEquals("protector", blocked.targetActorId)
	}

	@Test
	fun `substitute fails at exact hp cost after skill use`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-fails-at-exact-hp-cost-after-skill-use",
			inputSummary = "使用者当前 HP 正好等于替身费用。",
			expectedSummary = "技能已经宣告并消耗 PP，但由于 HP 不多于费用，不建立替身也不扣除 HP。",
		)
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = participant("user", speed = 100, currentHp = 25, skill = substituteSkill()), second = participant("target", speed = 50))),
			listOf(BattleAction.UseSkill("user", skillId = 164, targetActorId = "user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("substitute-fails-at-exact-hp-cost-after-skill-use")
		assertEquals(25, resolved.participant("user")?.currentHp)
		assertEquals(0, resolved.participant("user")?.substituteHp)
		assertEquals(9, resolved.participant("user")?.skillSlot(164)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SubstituteStarted>())
	}

	@Test
	fun `substitute fails when user already has substitute after skill use`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-fails-when-user-already-has-substitute-after-skill-use",
			inputSummary = "使用者已经拥有替身时再次使用替身技能。",
			expectedSummary = "技能已经宣告并消耗 PP，但不覆盖既有替身，也不重复扣除 HP。",
		)
		val user = participant("user", speed = 100, currentHp = 75, skill = substituteSkill()).copy(substituteHp = 25)
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = user, second = participant("target", speed = 50))),
			listOf(BattleAction.UseSkill("user", skillId = 164, targetActorId = "user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("substitute-fails-when-user-already-has-substitute-after-skill-use")
		assertEquals(75, resolved.participant("user")?.currentHp)
		assertEquals(25, resolved.participant("user")?.substituteHp)
		assertEquals(9, resolved.participant("user")?.skillSlot(164)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SubstituteStarted>())
	}

	@Test
	fun `substitute blocks opponent stat stage drop`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-blocks-opponent-stat-stage-drop",
			inputSummary = "目标已有替身，对手使用非声音类降能力变化技能。",
			expectedSummary = "替身阻止来自对手的能力阶级下降，目标能力阶级不变。",
		)
		val stageSkill = statStageDropSkill(skillId = 103, soundBased = false)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("stage-user", speed = 100, skill = stageSkill),
					second = participant("target", speed = 50, currentHp = 75).copy(substituteHp = 25),
				),
			),
			listOf(BattleAction.UseSkill("stage-user", skillId = 103, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("substitute-blocks-opponent-stat-stage-drop")
		assertEquals(0, resolved.participant("target")?.statStage(BattleStat.SPECIAL_DEFENSE))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageChanged>())
	}

	@Test
	fun `substitute allows ally stat stage effect`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-allows-ally-stat-stage-effect",
			inputSummary = "目标已有替身，同侧伙伴对其使用能力阶级变化技能。",
			expectedSummary = "替身只阻止对手技能，同侧伙伴的能力阶级效果正常生效。",
		)
		val stageSkill = statStageDropSkill(skillId = 104, soundBased = false)
		val resolved = engine.resolveTurn(
			engine.start(
				doubleInitialState(
					firstA = participant("stage-user", speed = 100, skill = stageSkill),
					firstB = participant("ally-target", speed = 50, currentHp = 75).copy(substituteHp = 25),
					secondA = participant("opponent-left", speed = 80),
					secondB = participant("opponent-right", speed = 70),
				),
			),
			listOf(BattleAction.UseSkill("stage-user", skillId = 104, targetActorId = "ally-target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("substitute-allows-ally-stat-stage-effect")
		assertEquals(-2, resolved.participant("ally-target")?.statStage(BattleStat.SPECIAL_DEFENSE))
		assertEquals("ally-target", resolved.events.filterIsInstance<BattleEvent.StatStageChanged>().single().targetActorId)
	}

	@Test
	fun `substitute does not block user self healing skill`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-does-not-block-user-self-healing-skill",
			inputSummary = "使用者已有替身并对自己使用回复类变化技能。",
			expectedSummary = "替身不阻止使用者自己的回复技能，本体 HP 回复且替身 HP 保持不变。",
		)
		val healer = participant("healer", speed = 100, currentHp = 50, skill = selfHealingSkill()).copy(substituteHp = 25)
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = healer, second = participant("target", speed = 50))),
			listOf(BattleAction.UseSkill("healer", skillId = 105, targetActorId = "healer")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("substitute-does-not-block-user-self-healing-skill")
		assertEquals(100, resolved.participant("healer")?.currentHp)
		assertEquals(25, resolved.participant("healer")?.substituteHp)
		assertEquals(50, resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single().amount)
	}

	@Test
	fun `grass target blocks powder status skill before duration random`() {
		val scenario = publicBattleRuleScenario(
			name = "grass-target-blocks-powder-status-skill-before-duration-random",
			inputSummary = "对草属性目标使用粉末类睡眠技能。",
			expectedSummary = "草属性天然免疫粉末类技能，阻挡发生在睡眠持续时间随机之前。",
		)
		val powderSkill = damagingSkill(
			skillId = 106,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			powderBased = true,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.SLEEP,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val random = ScriptedBattleRandom(emptyList())
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = participant("powder-user", speed = 100, skill = powderSkill), second = participant("grass-target", speed = 50, elementId = 12))),
			listOf(BattleAction.UseSkill("powder-user", skillId = 106, targetActorId = "grass-target")),
			random,
		)

		scenario.assertNamed("grass-target-blocks-powder-status-skill-before-duration-random")
		assertEquals(null, resolved.participant("grass-target")?.majorStatus)
		assertEquals("grass-target", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByElement>().single().targetActorId)
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `sound immunity ability blocks sound skill before damage random`() {
		val scenario = publicBattleRuleScenario(
			name = "sound-immunity-ability-blocks-sound-skill-before-damage-random",
			inputSummary = "目标拥有声音类技能免疫特性，对手使用声音类伤害技能。",
			expectedSummary = "特性在命中后伤害前阻挡技能，不消费要害或伤害随机数。",
		)
		val soundSkill = damagingSkill(skillId = 107, soundBased = true)
		val random = ScriptedBattleRandom(emptyList())
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("sound-user", speed = 100, skill = soundSkill),
					second = participant(
						"soundproof-target",
						speed = 50,
						abilityEffects = listOf(BattleAbilityEffect.SoundBasedSkillImmunity),
					),
				),
			),
			listOf(BattleAction.UseSkill("sound-user", skillId = 107, targetActorId = "soundproof-target")),
			random,
		)

		scenario.assertNamed("sound-immunity-ability-blocks-sound-skill-before-damage-random")
		assertEquals(100, resolved.participant("soundproof-target")?.currentHp)
		assertEquals("soundproof-target", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>().single().targetActorId)
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `type immunity blocks fixed damage before direct damage`() {
		val scenario = publicBattleRuleScenario(
			name = "type-immunity-blocks-fixed-damage-before-direct-damage",
			inputSummary = "固定伤害技能的属性对目标属性相性为 0。",
			expectedSummary = "属性免疫在固定伤害前短路，目标 HP 不变，事件记录 0 伤害。",
		)
		val fixedSkill = damagingSkill(
			skillId = 108,
			elementId = 1,
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(40),
		)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("fixed-user", speed = 100, elementId = 1, skill = fixedSkill),
					second = participant("immune-target", speed = 50, elementId = 2),
					rules = BattleRuleSnapshot(elementChart = ElementEffectivenessChart(mapOf(1L to mapOf(2L to 0.0)))),
				),
			),
			listOf(BattleAction.UseSkill("fixed-user", skillId = 108, targetActorId = "immune-target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("type-immunity-blocks-fixed-damage-before-direct-damage")
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		assertEquals(0, damage.amount)
		assertEquals(0.0, damage.effectiveness)
		assertEquals(100, resolved.participant("immune-target")?.currentHp)
	}

	private fun substituteSkill() =
		damagingSkill(
			skillId = 164,
			name = "替身测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			hpEffects = listOf(BattleSkillHpEffect.CreateSubstitute(numerator = 1, denominator = 4)),
		).copy(remainingPp = 10, maxPp = 10)

	private fun statStageDropSkill(skillId: Long, soundBased: Boolean) =
		damagingSkill(
			skillId = skillId,
			name = "降防测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			soundBased = soundBased,
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.SPECIAL_DEFENSE,
					target = BattleEffectTarget.TARGET,
					stageDelta = -2,
					chancePercent = 100,
				),
			),
		).copy(remainingPp = 10, maxPp = 10)

	private fun selfHealingSkill() =
		damagingSkill(
			skillId = 105,
			name = "自我回复测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			hpEffects = listOf(BattleSkillHpEffect.SelfHealMaxHpFraction(numerator = 1, denominator = 2)),
		).copy(remainingPp = 10, maxPp = 10)
}
