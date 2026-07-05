package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleOneHitKnockOut
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证一击必杀类直接伤害技能。
 *
 * 场景类型：状态机级 场景。
 * 参考来源类型：公开成熟对战引擎和公开规则说明中的一击必杀技能规则；本测试只固化输入、随机脚本和可观察事件，
 * 不复制外部实现代码。
 * 固定随机序列意图：一击必杀有自己的命中率公式，会消费一次命中随机数；命中后不消费击中要害或伤害浮动随机数。
 * 验证重点：目标等级高于使用者时失败且不耗随机数；命中率使用基础值加等级差并忽略命中/闪避阶级；命中后造成
 * 等于目标当前 HP 的直接伤害；同属性敏感例外可以降低基础命中率并阻止同属性目标。
 */
class BattleOneHitKnockOutSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `one hit knock out deals target current hp after special accuracy hit`() {
		val scenario = publicBattleRuleScenario(
			name = "one-hit-knockout-damage-deals-target-current-hp-after-special-accuracy-hit",
			inputSummary = "使用者与目标同为 50 级，一击必杀技能以 30% 专用命中率命中当前 HP 为 83 的目标。",
			expectedSummary = "技能不进入普通伤害公式，命中后直接造成 83 点伤害并让目标倒下。",
		)
		val random = ScriptedBattleRandom(listOf(29))
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, level = 50, skill = oneHitKnockOutSkill()),
					second = participant("target", speed = 80, level = 50, currentHp = 83),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 12, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("one-hit-knockout-damage-deals-target-current-hp-after-special-accuracy-hit")
		assertEquals(0, resolved.participant("target")?.currentHp)
		assertEquals(83, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(listOf("accuracy for 12"), random.consumedReasons())
	}

	@Test
	fun `one hit knock out fails against higher level target before accuracy roll`() {
		val scenario = publicBattleRuleScenario(
			name = "one-hit-knockout-damage-fails-against-higher-level-target-before-accuracy-roll",
			inputSummary = "50 级使用者对 51 级目标使用一击必杀技能。",
			expectedSummary = "技能因目标等级更高而失败，不产生命中随机数，也不会退回普通伤害公式。",
		)
		val random = ScriptedBattleRandom(emptyList())
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, level = 50, skill = oneHitKnockOutSkill()),
					second = participant("target", speed = 80, level = 51),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 12, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("one-hit-knockout-damage-fails-against-higher-level-target-before-accuracy-roll")
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals("target-level-greater-than-user-level", resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `one hit knock out accuracy ignores accuracy and evasion stages`() {
		val scenario = publicBattleRuleScenario(
			name = "one-hit-knockout-damage-accuracy-ignores-accuracy-and-evasion-stages",
			inputSummary = "60 级使用者命中阶级为 -6，50 级目标闪避阶级为 +6；一击必杀技能命中随机数落在 40% 内。",
			expectedSummary = "专用命中率按 30% 加等级差得到 40%，不读取双方命中和闪避阶级，技能仍然命中并造成直接伤害。",
		)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, level = 60, skill = oneHitKnockOutSkill())
						.copy(statStages = mapOf(BattleStat.ACCURACY to -6)),
					second = participant("target", speed = 80, level = 50)
						.copy(statStages = mapOf(BattleStat.EVASION to 6)),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 12, targetActorId = "target")),
			ScriptedBattleRandom(listOf(39)),
		)

		scenario.assertNamed("one-hit-knockout-damage-accuracy-ignores-accuracy-and-evasion-stages")
		assertEquals(0, resolved.participant("target")?.currentHp)
		assertEquals(100, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	@Test
	fun `same element sensitive one hit knock out uses lower base accuracy for off element user`() {
		val scenario = publicBattleRuleScenario(
			name = "same-element-sensitive-one-hit-knockout-damage-uses-lower-base-accuracy-for-off-element-user",
			inputSummary = "非同属性使用者使用同属性敏感一击必杀技能，双方等级相同，命中随机数为 25。",
			expectedSummary = "该技能使用 20% 基础命中率；随机数 25 会未命中，证明没有错误套用 30% 基础命中率。",
		)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, level = 50, elementId = 1, skill = sensitiveOneHitKnockOutSkill()),
					second = participant("target", speed = 80, level = 50, elementId = 2),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 329, targetActorId = "target")),
			ScriptedBattleRandom(listOf(24)),
		)

		scenario.assertNamed("same-element-sensitive-one-hit-knockout-damage-uses-lower-base-accuracy-for-off-element-user")
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals(25, resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single().accuracyRoll)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
	}

	@Test
	fun `same element sensitive one hit knock out is blocked by same element target before accuracy roll`() {
		val scenario = publicBattleRuleScenario(
			name = "same-element-sensitive-one-hit-knockout-damage-blocks-same-element-target-before-accuracy-roll",
			inputSummary = "同属性敏感一击必杀技能打向拥有技能同属性的目标。",
			expectedSummary = "目标在命中判定前触发属性天然无效，不消费命中随机数，也不会造成直接伤害。",
		)
		val random = ScriptedBattleRandom(emptyList())
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, level = 50, elementId = 15, skill = sensitiveOneHitKnockOutSkill()),
					second = participant("target", speed = 80, level = 50, elementId = 15),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 329, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("same-element-sensitive-one-hit-knockout-damage-blocks-same-element-target-before-accuracy-roll")
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals(15, resolved.events.filterIsInstance<BattleEvent.SkillBlockedByElement>().single().elementId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `accuracy lock makes next one hit knock out skip accuracy roll`() {
		val scenario = publicBattleRuleScenario(
			name = "accuracy-lock-makes-next-one-hit-knockout-skip-accuracy-roll",
			inputSummary = "使用者先用命中锁定类变化技能命中 50 级目标，下一回合再对同一目标使用一击必杀技能。",
			expectedSummary = "命中锁定跨过当前回合末保留到下一回合；一击必杀仍通过等级条件，但跳过 30% 命中随机数并直接造成目标当前 HP 伤害。",
		)
		val initial = engine.start(
			initialState(
				first = participant("user", speed = 100, level = 50, skill = accuracyLockSkill())
					.copy(skillSlots = listOf(accuracyLockSkill(), oneHitKnockOutSkill())),
				second = participant("target", speed = 80, level = 50, currentHp = 83),
			),
		)
		val afterLock = engine.resolveTurn(
			initial,
			listOf(BattleAction.UseSkill("user", skillId = 170, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)
		val random = ScriptedBattleRandom(emptyList())
		val resolved = engine.resolveTurn(
			afterLock,
			listOf(BattleAction.UseSkill("user", skillId = 12, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("accuracy-lock-makes-next-one-hit-knockout-skip-accuracy-roll")
		assertEquals("target", afterLock.participant("user")?.accuracyLockTargetActorId)
		assertEquals(1, afterLock.participant("user")?.accuracyLockTurnsRemaining)
		assertEquals(0, resolved.participant("target")?.currentHp)
		assertEquals(null, resolved.participant("user")?.accuracyLockTargetActorId)
		assertEquals(0, resolved.participant("user")?.accuracyLockTurnsRemaining)
		assertEquals(83, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `multiple users can lock accuracy on the same target`() {
		val scenario = publicBattleRuleScenario(
			name = "multiple-users-can-lock-accuracy-on-the-same-target",
			inputSummary = "双打中两个使用者同回合分别对同一个目标使用命中锁定类变化技能。",
			expectedSummary = "现代规则允许多个使用者同时锁定同一目标；第二个锁定不会覆盖第一个使用者的锁定状态。",
		)
		val resolved = engine.resolveTurn(
			engine.start(
				doubleInitialState(
					firstA = participant("user-a", speed = 100, skill = accuracyLockSkill()),
					firstB = participant("user-b", speed = 90, skill = accuracyLockSkill()),
					secondA = participant("target", speed = 80),
					secondB = participant("observer", speed = 70),
				),
			),
			listOf(
				BattleAction.UseSkill("user-a", skillId = 170, targetActorId = "target"),
				BattleAction.UseSkill("user-b", skillId = 170, targetActorId = "target"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("multiple-users-can-lock-accuracy-on-the-same-target")
		assertEquals("target", resolved.participant("user-a")?.accuracyLockTargetActorId)
		assertEquals("target", resolved.participant("user-b")?.accuracyLockTargetActorId)
		assertEquals(1, resolved.participant("user-a")?.accuracyLockTurnsRemaining)
		assertEquals(1, resolved.participant("user-b")?.accuracyLockTurnsRemaining)
		assertEquals(2, resolved.events.filterIsInstance<BattleEvent.AccuracyLockStarted>().size)
	}

	@Test
	fun `accuracy lock fails when already active on current target`() {
		val scenario = publicBattleRuleScenario(
			name = "accuracy-lock-fails-when-already-active-on-current-target",
			inputSummary = "使用者已经锁定当前目标后，下一回合没有先消费锁定，而是再次对同一目标使用命中锁定类变化技能。",
			expectedSummary = "重复锁定当前目标会失败，不产生新的锁定事件，也不会刷新旧锁定的剩余时间；旧锁定按回合末正常过期。",
		)
		val afterFirstLock = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, level = 50, skill = accuracyLockSkill()),
					second = participant("target", speed = 80, level = 50),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 170, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)
		val resolved = engine.resolveTurn(
			afterFirstLock,
			listOf(BattleAction.UseSkill("user", skillId = 170, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("accuracy-lock-fails-when-already-active-on-current-target")
		assertEquals(null, resolved.participant("user")?.accuracyLockTargetActorId)
		assertEquals(0, resolved.participant("user")?.accuracyLockTurnsRemaining)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.AccuracyLockStarted>().size)
		assertEquals(
			"accuracy-lock-already-active",
			resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	@Test
	fun `accuracy lock fails against target behind substitute`() {
		val scenario = publicBattleRuleScenario(
			name = "accuracy-lock-fails-against-target-behind-substitute",
			inputSummary = "目标已经拥有替身，使用者对该目标使用命中锁定类变化技能。",
			expectedSummary = "替身阻挡命中锁定效果；使用者不会获得必中运行态，也不会产生锁定开始事件。",
		)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, level = 50, skill = accuracyLockSkill()),
					second = participant("target", speed = 80, level = 50).copy(substituteHp = 25),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 170, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("accuracy-lock-fails-against-target-behind-substitute")
		assertEquals(null, resolved.participant("user")?.accuracyLockTargetActorId)
		assertEquals(0, resolved.participant("user")?.accuracyLockTurnsRemaining)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.AccuracyLockStarted>())
		assertEquals("target-behind-substitute", resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason)
	}

	@Test
	fun `accuracy lock ends when locked target switches out`() {
		val scenario = publicBattleRuleScenario(
			name = "accuracy-lock-ends-when-locked-target-switches-out",
			inputSummary = "使用者锁定目标后，下一回合目标先替换下场；使用者仍对原目标槽位使用一击必杀技能。",
			expectedSummary = "锁定绑定的是离场前那个成员而不是站位槽；新上场成员不继承必中效果，技能必须重新消费命中随机数。",
		)
		val afterLock = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, level = 50, skill = accuracyLockSkill())
						.copy(skillSlots = listOf(accuracyLockSkill(), oneHitKnockOutSkill())),
					second = participant("target", speed = 80, level = 50),
					secondBench = listOf(participant("reserve", speed = 70, level = 50)),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 170, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)
		val random = ScriptedBattleRandom(listOf(99))
		val resolved = engine.resolveTurn(
			afterLock,
			listOf(
				BattleAction.SwitchParticipant("target", targetActorId = "reserve"),
				BattleAction.UseSkill("user", skillId = 12, targetActorId = "target"),
			),
			random,
		)

		scenario.assertNamed("accuracy-lock-ends-when-locked-target-switches-out")
		assertEquals(null, resolved.participant("user")?.accuracyLockTargetActorId)
		assertEquals(0, resolved.participant("user")?.accuracyLockTurnsRemaining)
		assertEquals(100, resolved.participant("reserve")?.currentHp)
		assertEquals(100, resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single().accuracyRoll)
		assertEquals(listOf("accuracy for 12"), random.consumedReasons())
	}

	@Test
	fun `accuracy lock does not bypass protection before one hit knock out`() {
		val scenario = publicBattleRuleScenario(
			name = "accuracy-lock-does-not-bypass-protection-before-one-hit-knockout",
			inputSummary = "使用者锁定目标后，下一回合目标先建立保护屏障，使用者再对该目标使用一击必杀技能。",
			expectedSummary = "保护属于命中前 gate，仍然早于锁定命中判定阻挡技能；目标不受伤害，也不消费一击必杀命中随机数。",
		)
		val afterLock = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, level = 50, skill = accuracyLockSkill())
						.copy(skillSlots = listOf(accuracyLockSkill(), oneHitKnockOutSkill())),
					second = participant("target", speed = 80, level = 50, skill = protectionSkill()),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 170, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)
		val random = ScriptedBattleRandom(emptyList())
		val resolved = engine.resolveTurn(
			afterLock,
			listOf(
				BattleAction.UseSkill("target", skillId = 2, targetActorId = "target"),
				BattleAction.UseSkill("user", skillId = 12, targetActorId = "target"),
			),
			random,
		)

		scenario.assertNamed("accuracy-lock-does-not-bypass-protection-before-one-hit-knockout")
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals(12, resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single().skillId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `accuracy lock does not bypass one hit knock out higher level failure`() {
		val scenario = publicBattleRuleScenario(
			name = "accuracy-lock-does-not-bypass-one-hit-knockout-higher-level-failure",
			inputSummary = "使用者锁定目标后，下一回合对等级更高的同一目标使用一击必杀技能。",
			expectedSummary = "目标等级更高是技能失败条件，不是命中率问题；锁定效果不会绕过该失败，也不会消费命中随机数。",
		)
		val afterLock = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, level = 50, skill = accuracyLockSkill())
						.copy(skillSlots = listOf(accuracyLockSkill(), oneHitKnockOutSkill())),
					second = participant("target", speed = 80, level = 51),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 170, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)
		val random = ScriptedBattleRandom(emptyList())
		val resolved = engine.resolveTurn(
			afterLock,
			listOf(BattleAction.UseSkill("user", skillId = 12, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("accuracy-lock-does-not-bypass-one-hit-knockout-higher-level-failure")
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals("target-level-greater-than-user-level", resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(emptyList(), random.consumedReasons())
	}

	private fun accuracyLockSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 170,
			name = "命中锁定测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = null,
			locksAccuracyOnTarget = true,
		)

	private fun oneHitKnockOutSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 12,
			name = "一击必杀测试",
			damageClass = BattleDamageClass.PHYSICAL,
			power = null,
			accuracy = 30,
			oneHitKnockOut = BattleOneHitKnockOut(),
		)

	private fun sensitiveOneHitKnockOutSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 329,
			name = "同属性敏感一击必杀测试",
			elementId = 15,
			damageClass = BattleDamageClass.SPECIAL,
			power = null,
			accuracy = 30,
			oneHitKnockOut = BattleOneHitKnockOut(
				baseAccuracyPercent = 20,
				sameElementUserBaseAccuracyPercent = 30,
				blocksSameElementTarget = true,
			),
		)

}
