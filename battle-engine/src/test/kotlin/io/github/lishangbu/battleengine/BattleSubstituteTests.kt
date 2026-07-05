package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证替身核心状态机。
 *
 * 场景类型：技能流程和短期防护状态 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。替身通过支付使用者最大 HP 的 1/4 建立；对手普通伤害先扣
 * 替身 HP，不改变本体 HP；替身存在时会阻止对手技能带来的主要异常状态、混乱和畏缩等临时状态。
 * 验证重点：替身事件不混用本体伤害事件，破裂后清空替身 HP，状态阻止不会消费状态私有随机数。
 */
class BattleSubstituteTests {
	private val engine = BattleEngine()

	@Test
	fun `substitute skill pays quarter max hp and absorbs later damage until broken`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-pays-quarter-max-hp-and-absorbs-damage-until-broken",
			inputSummary = "较快成员使用替身，较慢对手同回合使用普通攻击命中该成员。",
			expectedSummary = "使用者支付最大 HP 的 1/4 建立 25 HP 替身；对手伤害先扣替身，替身破裂，本体 HP 保持 75。",
		)
		val state = engine.start(
			initialState(
				first = participant("substitute-user", speed = 100, skill = substituteSkill()),
				second = participant("attacker", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("substitute-user", skillId = 164, targetActorId = "substitute-user"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "substitute-user"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("substitute-pays-quarter-max-hp-and-absorbs-damage-until-broken")
		assertEquals(75, resolved.participant("substitute-user")?.currentHp)
		assertEquals(0, resolved.participant("substitute-user")?.substituteHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		val started = resolved.events.filterIsInstance<BattleEvent.SubstituteStarted>().single()
		assertEquals(25, started.hpCost)
		assertEquals(25, started.substituteHp)
		val substituteDamage = resolved.events.filterIsInstance<BattleEvent.SubstituteDamageApplied>().single()
		assertEquals(25, substituteDamage.amount)
		assertEquals(0, substituteDamage.substituteHpRemaining)
		assertEquals("substitute-user", resolved.events.filterIsInstance<BattleEvent.SubstituteBroken>().single().actorId)
	}

	@Test
	fun `substitute keeps remaining hp when absorbed damage is lower than substitute hp`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-keeps-remaining-hp-after-partial-damage",
			inputSummary = "较快成员使用替身，较慢对手使用低威力普通攻击命中。",
			expectedSummary = "替身承受 15 点伤害后仍剩余 10 HP，本体 HP 仍为建立替身后的 75。",
		)
		val weakSkill = damagingSkill(name = "弱攻击测试", power = 20)
		val state = engine.start(
			initialState(
				first = participant("substitute-user", speed = 100, skill = substituteSkill()),
				second = participant("attacker", speed = 50, skill = weakSkill),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("substitute-user", skillId = 164, targetActorId = "substitute-user"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "substitute-user"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("substitute-keeps-remaining-hp-after-partial-damage")
		assertEquals(75, resolved.participant("substitute-user")?.currentHp)
		assertEquals(10, resolved.participant("substitute-user")?.substituteHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SubstituteBroken>())
		val substituteDamage = resolved.events.filterIsInstance<BattleEvent.SubstituteDamageApplied>().single()
		assertEquals(15, substituteDamage.amount)
		assertEquals(10, substituteDamage.substituteHpRemaining)
	}

	@Test
	fun `substitute fails when user hp is not greater than substitute cost`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-fails-when-user-hp-is-not-greater-than-cost",
			inputSummary = "使用者当前 HP 等于建立替身所需的 1/4 最大 HP，并尝试使用替身。",
			expectedSummary = "替身技能已经宣告并消耗 PP，但因 HP 不足失败；本体 HP 不变，也不建立替身。",
		)
		val state = engine.start(
			initialState(
				first = participant("substitute-user", speed = 100, currentHp = 25, skill = substituteSkill()),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("substitute-user", skillId = 164, targetActorId = "substitute-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("substitute-fails-when-user-hp-is-not-greater-than-cost")
		assertEquals(25, resolved.participant("substitute-user")?.currentHp)
		assertEquals(0, resolved.participant("substitute-user")?.substituteHp)
		assertEquals(9, resolved.participant("substitute-user")?.skillSlot(164)?.remainingPp)
		assertEquals(164, resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single().skillId)
		assertEquals("insufficient-hp-for-substitute", resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SubstituteStarted>())
	}

	@Test
	fun `substitute fails when user already has substitute`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-fails-when-user-already-has-substitute",
			inputSummary = "使用者已经拥有 25 HP 替身，又尝试再次使用替身。",
			expectedSummary = "替身技能已经宣告并消耗 PP，但因已有替身失败；既有替身和本体 HP 都保持不变。",
		)
		val state = engine.start(
			initialState(
				first = participant("substitute-user", speed = 100, currentHp = 75, skill = substituteSkill())
					.copy(substituteHp = 25),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("substitute-user", skillId = 164, targetActorId = "substitute-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("substitute-fails-when-user-already-has-substitute")
		assertEquals(75, resolved.participant("substitute-user")?.currentHp)
		assertEquals(25, resolved.participant("substitute-user")?.substituteHp)
		assertEquals(9, resolved.participant("substitute-user")?.skillSlot(164)?.remainingPp)
		assertEquals("substitute-already-active", resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SubstituteStarted>())
	}

	/**
	 * 固定“替身挡住伤害时不进入本体保命窗口”的边界。
	 *
	 * 满 HP 保命特性只应该在本体即将被技能伤害写入到 0 HP 时触发。若目标存在替身且本次技能不能穿透替身，
	 * 即使直接伤害数值远大于目标最大 HP，也只能扣替身 HP；本体没有发生 HP 写入，所以不能追加保命事件，
	 * 也不能把替身之外的溢出伤害继续传给本体。
	 */
	@Test
	fun `substitute absorbing lethal direct damage does not trigger body fatal survival`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-absorbing-lethal-direct-damage-does-not-trigger-body-survival",
			inputSummary = "目标满 HP、拥有满 HP 保命特性并已有 25 HP 替身；对手使用 200 点固定直接伤害技能。",
			expectedSummary = "固定伤害只打破 25 HP 替身，本体 HP 保持 100；不出现本体伤害事件，也不触发保命事件。",
		)
		val fatalSkill = damagingSkill(
			skillId = 9012,
			name = "替身保命边界测试",
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(200),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = fatalSkill),
				second = participant(
					"protected-target",
					speed = 50,
					abilityId = 5,
					abilityEffects = listOf(BattleAbilityEffect.SurviveFatalDamageAtFullHp()),
				).copy(substituteHp = 25),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 9012, targetActorId = "protected-target")),
			ScriptedBattleRandom(emptyList()),
		)
		val target = requireNotNull(resolved.participant("protected-target"))
		val substituteDamage = resolved.events.filterIsInstance<BattleEvent.SubstituteDamageApplied>().single()

		scenario.assertNamed("substitute-absorbing-lethal-direct-damage-does-not-trigger-body-survival")
		assertEquals(100, target.currentHp)
		assertEquals(0, target.substituteHp)
		assertEquals(25, substituteDamage.amount)
		assertEquals(0, substituteDamage.substituteHpRemaining)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.FatalDamageSurvived>())
		assertEquals("protected-target", resolved.events.filterIsInstance<BattleEvent.SubstituteBroken>().single().actorId)
	}

	@Test
	fun `substitute blocks opponent major status without consuming status duration random`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-blocks-opponent-major-status",
			inputSummary = "目标已经拥有替身，对手使用必定造成睡眠的变化技能。",
			expectedSummary = "睡眠被替身阻止，目标不获得主要异常状态，也不会消费睡眠持续时间随机数。",
		)
		val sleepSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.SLEEP,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val random = ScriptedBattleRandom(emptyList())
		val state = engine.start(
			initialState(
				first = participant("status-user", speed = 100, skill = sleepSkill),
				second = participant("protected-target", speed = 50, currentHp = 75).copy(substituteHp = 25),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("status-user", skillId = 1, targetActorId = "protected-target")),
			random,
		)

		scenario.assertNamed("substitute-blocks-opponent-major-status")
		assertEquals(emptyList(), random.consumedReasons())
		assertEquals(null, resolved.participant("protected-target")?.majorStatus)
		assertEquals(25, resolved.participant("protected-target")?.substituteHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
		val blocked = resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>().single()
		assertEquals(BattleMajorStatus.SLEEP, blocked.status)
		assertEquals(BattleStatusBlockReason.SUBSTITUTE, blocked.reason)
	}

	@Test
	fun `substitute blocks opponent volatile status before confusion duration random`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-blocks-opponent-volatile-status",
			inputSummary = "目标已经拥有替身，对手使用必定造成混乱的变化技能。",
			expectedSummary = "混乱被替身阻止，目标不写入混乱计数，也不会消费混乱持续时间随机数。",
		)
		val confusionSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.CONFUSION,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val random = ScriptedBattleRandom(emptyList())
		val state = engine.start(
			initialState(
				first = participant("status-user", speed = 100, skill = confusionSkill),
				second = participant("protected-target", speed = 50, currentHp = 75).copy(substituteHp = 25),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("status-user", skillId = 1, targetActorId = "protected-target")),
			random,
		)

		scenario.assertNamed("substitute-blocks-opponent-volatile-status")
		assertEquals(emptyList(), random.consumedReasons())
		assertEquals(0, resolved.participant("protected-target")?.confusionTurnsRemaining)
		assertEquals(25, resolved.participant("protected-target")?.substituteHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>())
		val blocked = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplicationBlocked>().single()
		assertEquals(BattleVolatileStatus.CONFUSION, blocked.status)
		assertEquals(BattleStatusBlockReason.SUBSTITUTE, blocked.reason)
	}

	@Test
	fun `sound damaging skill bypasses substitute and damages target body`() {
		val scenario = publicBattleRuleScenario(
			name = "sound-damage-skill-bypasses-substitute",
			inputSummary = "目标已经拥有替身，对手使用声音类伤害技能。",
			expectedSummary = "声音类伤害技能穿过替身直接伤害本体，替身 HP 保持不变。",
		)
		val soundSkill = damagingSkill(name = "声音伤害测试", soundBased = true)
		val state = engine.start(
			initialState(
				first = participant("sound-user", speed = 100, skill = soundSkill),
				second = participant("protected-target", speed = 50, currentHp = 75).copy(substituteHp = 25),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("sound-user", skillId = 1, targetActorId = "protected-target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("sound-damage-skill-bypasses-substitute")
		assertEquals(47, resolved.participant("protected-target")?.currentHp)
		assertEquals(25, resolved.participant("protected-target")?.substituteHp)
		assertEquals(28, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SubstituteDamageApplied>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SubstituteBroken>())
	}

	@Test
	fun `sound status skill bypasses substitute while ordinary stat stage skill is blocked`() {
		val scenario = publicBattleRuleScenario(
			name = "sound-status-skill-bypasses-substitute",
			inputSummary = "目标已经拥有替身；一个普通降能力变化技和一个声音类降能力变化技分别命中该目标。",
			expectedSummary = "普通降能力变化技被替身阻止；声音类降能力变化技穿过替身并实际降低目标能力阶级。",
		)
		val ordinaryStageSkill = statStageDropSkill(skillId = 103, name = "普通降防测试", soundBased = false)
		val soundStageSkill = statStageDropSkill(skillId = 319, name = "声音降防测试", soundBased = true)
		val ordinaryBlocked = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("stage-user", speed = 100, skill = ordinaryStageSkill),
					second = participant("protected-target", speed = 50, currentHp = 75).copy(substituteHp = 25),
				),
			),
			listOf(BattleAction.UseSkill("stage-user", skillId = 103, targetActorId = "protected-target")),
			ScriptedBattleRandom(emptyList()),
		)

		val soundBypassed = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("stage-user", speed = 100, skill = soundStageSkill),
					second = participant("protected-target", speed = 50, currentHp = 75).copy(substituteHp = 25),
				),
			),
			listOf(BattleAction.UseSkill("stage-user", skillId = 319, targetActorId = "protected-target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("sound-status-skill-bypasses-substitute")
		assertEquals(0, ordinaryBlocked.participant("protected-target")?.statStage(BattleStat.SPECIAL_DEFENSE))
		assertEquals(25, ordinaryBlocked.participant("protected-target")?.substituteHp)
		assertEquals(emptyList(), ordinaryBlocked.events.filterIsInstance<BattleEvent.StatStageChanged>())
		assertEquals(-2, soundBypassed.participant("protected-target")?.statStage(BattleStat.SPECIAL_DEFENSE))
		assertEquals(25, soundBypassed.participant("protected-target")?.substituteHp)
		val changed = soundBypassed.events.filterIsInstance<BattleEvent.StatStageChanged>().single()
		assertEquals("protected-target", changed.targetActorId)
		assertEquals(BattleStat.SPECIAL_DEFENSE, changed.stat)
		assertEquals(-2, changed.delta)
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

	private fun statStageDropSkill(skillId: Long, name: String, soundBased: Boolean) =
		damagingSkill(
			skillId = skillId,
			name = name,
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
}
