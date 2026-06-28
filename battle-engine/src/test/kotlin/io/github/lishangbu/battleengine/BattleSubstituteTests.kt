package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
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
 * 场景类型：技能流程和短期防护状态 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。替身通过支付使用者最大 HP 的 1/4 建立；对手普通伤害先扣
 * 替身 HP，不改变本体 HP；替身存在时会阻止对手技能带来的主要异常状态、混乱和畏缩等临时状态。
 * 验证重点：替身事件不混用本体伤害事件，破裂后清空替身 HP，状态阻止不会消费状态私有随机数。
 */
class BattleSubstituteTests {
	private val engine = BattleEngine()

	@Test
	fun `substitute skill pays quarter max hp and absorbs later damage until broken`() {
		val fixture = publicBattleRuleFixture(
			name = "substitute-pays-quarter-max-hp-and-absorbs-damage-until-broken",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Substitute_(move)",
			),
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

		fixture.assertNamed("substitute-pays-quarter-max-hp-and-absorbs-damage-until-broken")
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
		val fixture = publicBattleRuleFixture(
			name = "substitute-keeps-remaining-hp-after-partial-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Substitute_(move)",
			),
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

		fixture.assertNamed("substitute-keeps-remaining-hp-after-partial-damage")
		assertEquals(75, resolved.participant("substitute-user")?.currentHp)
		assertEquals(10, resolved.participant("substitute-user")?.substituteHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SubstituteBroken>())
		val substituteDamage = resolved.events.filterIsInstance<BattleEvent.SubstituteDamageApplied>().single()
		assertEquals(15, substituteDamage.amount)
		assertEquals(10, substituteDamage.substituteHpRemaining)
	}

	@Test
	fun `substitute blocks opponent major status without consuming status duration random`() {
		val fixture = publicBattleRuleFixture(
			name = "substitute-blocks-opponent-major-status",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Substitute_(move)",
			),
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

		fixture.assertNamed("substitute-blocks-opponent-major-status")
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
		val fixture = publicBattleRuleFixture(
			name = "substitute-blocks-opponent-volatile-status",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Substitute_(move)",
			),
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

		fixture.assertNamed("substitute-blocks-opponent-volatile-status")
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
		val fixture = publicBattleRuleFixture(
			name = "sound-damage-skill-bypasses-substitute",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Substitute_(move)",
				"https://bulbapedia.bulbagarden.net/wiki/Sound-based_move",
			),
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

		fixture.assertNamed("sound-damage-skill-bypasses-substitute")
		assertEquals(47, resolved.participant("protected-target")?.currentHp)
		assertEquals(25, resolved.participant("protected-target")?.substituteHp)
		assertEquals(28, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SubstituteDamageApplied>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SubstituteBroken>())
	}

	@Test
	fun `sound status skill bypasses substitute while ordinary stat stage skill is blocked`() {
		val fixture = publicBattleRuleFixture(
			name = "sound-status-skill-bypasses-substitute",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Substitute_(move)",
				"https://bulbapedia.bulbagarden.net/wiki/Metal_Sound_(move)",
			),
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

		fixture.assertNamed("sound-status-skill-bypasses-substitute")
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
