package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatStageOperationKind
import io.github.lishangbu.battleengine.model.BattleStatStageOperationTarget
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证现代主系列技能效果在成功、失败和边界值上的状态机行为。
 *
 * 场景类型：技能 HP 后效、强制替换和能力阶级附加/特殊操作 fixture。
 * 参考来源类型：公开成熟对战引擎技能资料、公开技能规则说明和公开场上状态资料。该批次只覆盖“技能效果本身”的
 * 可复用行为，不把命中、保护、天气场地或普通伤害公式重新计数。
 * 验证重点：吸取回复和自我回复按缺失 HP 夹取、低额吸取最少 1 点、反作用伤害按使用者当前 HP 夹取并尊重免疫、
 * 强制替换的随机消费、失败短路、替身阻挡和入场陷阱接续，以及能力阶级效果/操作在概率失败、边界值和无变化时
 * 不产生误导性事件。
 */
class BattleSkillEffectBoundaryPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `drain healing clamps to missing hp`() {
		val fixture = fixture(
			name = "drain-healing-clamps-to-missing-hp",
			inputSummary = "使用者只缺失 5 HP，吸取类伤害技能造成 28 点实际伤害。",
			expectedSummary = "理论吸取回复为 14，但实际技能回复事件夹取为缺失的 5 HP。",
		)
		val skill = drainingSkill()
		val state = engine.start(
			initialState(
				first = participant("drain-user", speed = 100, currentHp = 95, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("drain-user", skillId = 401, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("drain-healing-clamps-to-missing-hp")
		assertEquals(100, resolved.participant("drain-user")?.currentHp)
		assertEquals(5, resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single().amount)
	}

	@Test
	fun `drain healing minimum one after one damage`() {
		val fixture = fixture(
			name = "drain-healing-minimum-one-after-one-damage",
			inputSummary = "吸取类伤害技能只让目标实际损失 1 HP。",
			expectedSummary = "1/2 吸取比例向下取整会低于 1，但正伤害吸取回复仍至少产生 1 点回复。",
		)
		val skill = drainingSkill()
		val state = engine.start(
			initialState(
				first = participant("drain-user", speed = 100, currentHp = 50, skill = skill),
				second = participant("target", speed = 50, currentHp = 1),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("drain-user", skillId = 401, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("drain-healing-minimum-one-after-one-damage")
		assertEquals(51, resolved.participant("drain-user")?.currentHp)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single().amount)
	}

	@Test
	fun `drain healing skips full hp user`() {
		val fixture = fixture(
			name = "drain-healing-skips-full-hp-user",
			inputSummary = "满 HP 使用者用吸取类伤害技能命中目标。",
			expectedSummary = "技能仍造成伤害，但使用者没有缺失 HP，因此不产生技能回复事件。",
		)
		val skill = drainingSkill()
		val state = engine.start(
			initialState(
				first = participant("drain-user", speed = 100, currentHp = 100, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("drain-user", skillId = 401, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("drain-healing-skips-full-hp-user")
		assertEquals(72, resolved.participant("target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>())
	}

	@Test
	fun `recoil damage clamps to user current hp`() {
		val fixture = fixture(
			name = "recoil-damage-clamps-to-user-current-hp",
			inputSummary = "使用者只剩 5 HP，带 1/3 反作用伤害的技能造成 28 点实际伤害。",
			expectedSummary = "理论反作用伤害四舍五入为 9，但实际扣减被夹取到使用者剩余 5 HP。",
		)
		val skill = recoilSkill()
		val state = engine.start(
			initialState(
				first = participant("recoil-user", speed = 100, currentHp = 5, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("recoil-user", skillId = 402, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("recoil-damage-clamps-to-user-current-hp")
		assertEquals(0, resolved.participant("recoil-user")?.currentHp)
		val recoil = resolved.events.filterIsInstance<BattleEvent.SkillRecoilDamageApplied>().single()
		assertEquals(5, recoil.amount)
		assertEquals(28, recoil.sourceDamageAmount)
	}

	@Test
	fun `recoil damage immunity skips skill recoil`() {
		val fixture = fixture(
			name = "recoil-damage-immunity-skips-skill-recoil",
			inputSummary = "使用者拥有技能反作用伤害免疫效果，并用带 1/3 反作用伤害的技能命中目标。",
			expectedSummary = "目标正常受到伤害，使用者不会承受技能反作用伤害，也不产生反作用事件。",
		)
		val skill = recoilSkill()
		val state = engine.start(
			initialState(
				first = participant(
					"recoil-user",
					speed = 100,
					currentHp = 50,
					skill = skill,
					abilityEffects = listOf(BattleAbilityEffect.SkillRecoilDamageImmunity),
				),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("recoil-user", skillId = 402, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("recoil-damage-immunity-skips-skill-recoil")
		assertEquals(50, resolved.participant("recoil-user")?.currentHp)
		assertEquals(72, resolved.participant("target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillRecoilDamageApplied>())
	}

	@Test
	fun `self healing clamps to missing hp`() {
		val fixture = fixture(
			name = "self-healing-clamps-to-missing-hp",
			inputSummary = "使用者只缺失 20 HP，使用回复最大 HP 1/2 的变化技能。",
			expectedSummary = "理论回复为 50，但实际技能回复事件夹取为缺失的 20 HP。",
		)
		val skill = selfHealingSkill()
		val state = engine.start(
			initialState(
				first = participant("heal-user", speed = 100, currentHp = 80, skill = skill),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("heal-user", skillId = 403, targetActorId = "heal-user")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("self-healing-clamps-to-missing-hp")
		assertEquals(100, resolved.participant("heal-user")?.currentHp)
		assertEquals(20, resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single().amount)
	}

	@Test
	fun `self healing skips full hp user`() {
		val fixture = fixture(
			name = "self-healing-skips-full-hp-user",
			inputSummary = "满 HP 使用者使用回复最大 HP 1/2 的变化技能。",
			expectedSummary = "技能成功使用但 HP 没有变化，因此不产生技能回复事件。",
		)
		val skill = selfHealingSkill()
		val state = engine.start(
			initialState(
				first = participant("heal-user", speed = 100, currentHp = 100, skill = skill),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("heal-user", skillId = 403, targetActorId = "heal-user")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("self-healing-skips-full-hp-user")
		assertEquals(100, resolved.participant("heal-user")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>())
	}

	@Test
	fun `weather sensitive self healing uses default fraction without weather`() {
		val fixture = fixture(
			name = "weather-sensitive-self-healing-uses-default-fraction-without-weather",
			inputSummary = "无天气下使用天气变量回复技能，默认比例为最大 HP 的 1/2。",
			expectedSummary = "当前天气没有命中任何特殊比例，因此使用默认 1/2 回复。",
		)
		val skill = weatherSensitiveHealingSkill()
		val state = engine.start(
			initialState(
				first = participant("heal-user", speed = 100, currentHp = 20, skill = skill),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.NONE),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("heal-user", skillId = 404, targetActorId = "heal-user")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("weather-sensitive-self-healing-uses-default-fraction-without-weather")
		assertEquals(70, resolved.participant("heal-user")?.currentHp)
		assertEquals(50, resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single().amount)
	}

	@Test
	fun `forced switch single bench consumes no random`() {
		val fixture = fixture(
			name = "forced-switch-single-bench-consumes-no-random",
			inputSummary = "强制替换变化技能命中目标，目标侧只有一个可战斗后备成员。",
			expectedSummary = "唯一后备成员直接换入，不消费强制替换随机数。",
		)
		val skill = forceSwitchStatusSkill()
		val random = ScriptedBattleRandom(emptyList())
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill),
				second = participant("target", speed = 80),
				secondBench = listOf(participant("reserve", speed = 60)),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 405, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("forced-switch-single-bench-consumes-no-random")
		assertEquals(listOf("reserve"), resolved.sides.single { it.sideId == "side-b" }.activeActorIds)
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `forced switch skips when target side has no bench`() {
		val fixture = fixture(
			name = "forced-switch-skips-when-target-side-has-no-bench",
			inputSummary = "强制替换变化技能命中目标，但目标侧没有任何可战斗后备成员。",
			expectedSummary = "强制替换效果保持状态不变，不消费随机数，也不产生强制替换选择事件。",
		)
		val skill = forceSwitchStatusSkill()
		val random = ScriptedBattleRandom(emptyList())
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill),
				second = participant("target", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 405, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("forced-switch-skips-when-target-side-has-no-bench")
		assertEquals(listOf("target"), resolved.sides.single { it.sideId == "side-b" }.activeActorIds)
		assertEquals(emptyList(), random.consumedReasons())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.TargetForcedSwitchSelected>())
	}

	@Test
	fun `forced switch skips target fainted by damage`() {
		val fixture = fixture(
			name = "forced-switch-skips-target-fainted-by-damage",
			inputSummary = "伤害类强制替换技能命中只剩 1 HP 的目标，目标侧仍有可战斗后备成员。",
			expectedSummary = "目标已经因伤害倒下，强制替换效果短路，不再选择后备成员。",
		)
		val skill = forceSwitchDamageSkill()
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill),
				second = participant("target", speed = 80, currentHp = 1),
				secondBench = listOf(participant("reserve", speed = 60)),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 406, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("forced-switch-skips-target-fainted-by-damage")
		assertEquals(0, resolved.participant("target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.TargetForcedSwitchSelected>())
	}

	@Test
	fun `substitute blocks forced switch effect`() {
		val fixture = fixture(
			name = "substitute-blocks-forced-switch-effect",
			inputSummary = "目标拥有替身，强制替换变化技能成功使用并指向该目标。",
			expectedSummary = "对手技能效果被替身阻挡，目标侧上场席位保持不变，不消费强制替换随机数。",
		)
		val skill = forceSwitchStatusSkill()
		val random = ScriptedBattleRandom(emptyList())
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill),
				second = participant("target", speed = 80).copy(substituteHp = 25),
				secondBench = listOf(participant("reserve", speed = 60)),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 405, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("substitute-blocks-forced-switch-effect")
		assertEquals(listOf("target"), resolved.sides.single { it.sideId == "side-b" }.activeActorIds)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.TargetForcedSwitchSelected>())
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `forced switch triggers entry hazard on replacement`() {
		val fixture = fixture(
			name = "forced-switch-triggers-entry-hazard-on-replacement",
			inputSummary = "目标侧已有一层入场伤害陷阱，强制替换变化技能让唯一后备成员换入。",
			expectedSummary = "强制替换事件后继续复用普通换入流程，新上场成员立刻承受入场陷阱伤害。",
		)
		val skill = forceSwitchStatusSkill()
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill),
				second = participant("target", speed = 80),
				secondBench = listOf(participant("reserve", speed = 60)),
				secondSideEntryHazards = listOf(BattleSideEntryHazard(BattleSideEntryHazardKind.SPIKES)),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 405, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("forced-switch-triggers-entry-hazard-on-replacement")
		assertEquals(listOf("reserve"), resolved.sides.single { it.sideId == "side-b" }.activeActorIds)
		assertEquals(88, resolved.participant("reserve")?.currentHp)
		val switchIndex = resolved.events.indexOfFirst { it is BattleEvent.ParticipantSwitched }
		val hazardIndex = resolved.events.indexOfFirst { it is BattleEvent.EntryHazardDamageApplied }
		assertTrue(switchIndex >= 0)
		assertTrue(hazardIndex > switchIndex)
		assertEquals(12, resolved.events.filterIsInstance<BattleEvent.EntryHazardDamageApplied>().single().amount)
	}

	@Test
	fun `stat stage effect chance failure consumes random without change`() {
		val fixture = fixture(
			name = "stat-stage-effect-chance-failure-consumes-random-without-change",
			inputSummary = "技能声明 50% 概率降低目标攻击阶级，但概率随机掷出失败值。",
			expectedSummary = "概率随机被消费，目标能力阶级保持不变，不产生能力阶级变化事件。",
		)
		val skill = statStageEffectSkill(chancePercent = 50)
		val random = ScriptedBattleRandom(listOf(99))
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 407, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("stat-stage-effect-chance-failure-consumes-random-without-change")
		assertEquals(0, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageChanged>())
		assertEquals(listOf("stat stage chance for 407"), random.consumedReasons())
	}

	@Test
	fun `stat stage effect at upper bound produces no event`() {
		val fixture = fixture(
			name = "stat-stage-effect-at-upper-bound-produces-no-event",
			inputSummary = "使用者攻击阶级已经为 +6，又使用提升自身攻击 1 级的变化技能。",
			expectedSummary = "能力阶级被边界夹取后没有实际变化，因此不产生能力阶级变化事件。",
		)
		val skill = selfAttackBoostSkill()
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill).copy(
					statStages = mapOf(BattleStat.ATTACK to 6),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 408, targetActorId = "actor")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("stat-stage-effect-at-upper-bound-produces-no-event")
		assertEquals(6, resolved.participant("actor")?.statStage(BattleStat.ATTACK))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageChanged>())
	}

	@Test
	fun `stat stage operation chance failure consumes random without change`() {
		val fixture = fixture(
			name = "stat-stage-operation-chance-failure-consumes-random-without-change",
			inputSummary = "技能声明 50% 概率清除目标攻击阶级，但概率随机掷出失败值。",
			expectedSummary = "概率随机被消费，目标能力阶级保持原值，不产生清除事件。",
		)
		val skill = statStageOperationSkill(
			skillId = 409,
			operation = clearTarget(BattleStat.ATTACK, chancePercent = 50),
		)
		val random = ScriptedBattleRandom(listOf(99))
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill),
				second = participant("target", speed = 50).copy(statStages = mapOf(BattleStat.ATTACK to 3)),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 409, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("stat-stage-operation-chance-failure-consumes-random-without-change")
		assertEquals(3, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageCleared>())
		assertEquals(listOf("stat stage operation chance for 409"), random.consumedReasons())
	}

	@Test
	fun `clear operation at zero produces no event`() {
		val fixture = fixture(
			name = "clear-operation-at-zero-produces-no-event",
			inputSummary = "目标攻击阶级已经为 0，技能成功执行清除目标攻击阶级的特殊操作。",
			expectedSummary = "清除操作没有改变状态，因此不产生能力阶级清除事件。",
		)
		val skill = statStageOperationSkill(410, clearTarget(BattleStat.ATTACK))
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 410, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("clear-operation-at-zero-produces-no-event")
		assertEquals(0, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageCleared>())
	}

	@Test
	fun `copy operation with same stage produces no event`() {
		val fixture = fixture(
			name = "copy-operation-with-same-stage-produces-no-event",
			inputSummary = "使用者和目标攻击阶级都为 +2，使用者成功执行复制目标攻击阶级的特殊操作。",
			expectedSummary = "复制结果与使用者当前阶级一致，状态不变，也不产生复制事件。",
		)
		val skill = statStageOperationSkill(411, copyTargetToUser(BattleStat.ATTACK))
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill).copy(
					statStages = mapOf(BattleStat.ATTACK to 2),
				),
				second = participant("target", speed = 50).copy(
					statStages = mapOf(BattleStat.ATTACK to 2),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 411, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("copy-operation-with-same-stage-produces-no-event")
		assertEquals(2, resolved.participant("actor")?.statStage(BattleStat.ATTACK))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageCopied>())
	}

	@Test
	fun `swap operation with same stage produces no event`() {
		val fixture = fixture(
			name = "swap-operation-with-same-stage-produces-no-event",
			inputSummary = "使用者和目标攻击阶级都为 +2，使用者成功执行交换双方攻击阶级的特殊操作。",
			expectedSummary = "交换前后双方阶级一致，状态不变，也不产生交换事件。",
		)
		val skill = statStageOperationSkill(412, swapUserAndTarget(BattleStat.ATTACK))
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill).copy(
					statStages = mapOf(BattleStat.ATTACK to 2),
				),
				second = participant("target", speed = 50).copy(
					statStages = mapOf(BattleStat.ATTACK to 2),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 412, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("swap-operation-with-same-stage-produces-no-event")
		assertEquals(2, resolved.participant("actor")?.statStage(BattleStat.ATTACK))
		assertEquals(2, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageSwapped>())
	}

	@Test
	fun `invert operation at zero produces no event`() {
		val fixture = fixture(
			name = "invert-operation-at-zero-produces-no-event",
			inputSummary = "目标攻击阶级为 0，使用者成功执行取反目标攻击阶级的特殊操作。",
			expectedSummary = "0 阶级取反后仍为 0，状态不变，也不产生取反事件。",
		)
		val skill = statStageOperationSkill(413, invertTarget(BattleStat.ATTACK))
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 413, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("invert-operation-at-zero-produces-no-event")
		assertEquals(0, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageInverted>())
	}

	private fun fixture(
		name: String,
		inputSummary: String,
		expectedSummary: String,
	): PublicBattleRuleFixture =
		publicBattleRuleFixture(
			name = name,
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Stat_modifier",
			),
			inputSummary = inputSummary,
			expectedSummary = expectedSummary,
		)

	private fun drainingSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 401,
			name = "吸取边界测试",
			hpEffects = listOf(BattleSkillHpEffect.DrainDamage(numerator = 1, denominator = 2)),
		)

	private fun recoilSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 402,
			name = "反作用边界测试",
			hpEffects = listOf(BattleSkillHpEffect.RecoilByDamageDealt(numerator = 1, denominator = 3)),
		)

	private fun selfHealingSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 403,
			name = "自我回复边界测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			hpEffects = listOf(BattleSkillHpEffect.SelfHealMaxHpFraction(numerator = 1, denominator = 2)),
		)

	private fun weatherSensitiveHealingSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 404,
			name = "天气回复边界测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
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

	private fun forceSwitchStatusSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 405,
			name = "强制替换边界测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = null,
			affectedByProtect = false,
			forceTargetSwitch = true,
		)

	private fun forceSwitchDamageSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 406,
			name = "强制替换伤害测试",
			power = 40,
			forceTargetSwitch = true,
		)

	private fun statStageEffectSkill(chancePercent: Int): BattleSkillSlot =
		damagingSkill(
			skillId = 407,
			name = "概率降阶边界测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.ATTACK,
					target = BattleEffectTarget.TARGET,
					stageDelta = -1,
					chancePercent = chancePercent,
				),
			),
		)

	private fun selfAttackBoostSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 408,
			name = "边界强化测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.ATTACK,
					target = BattleEffectTarget.USER,
					stageDelta = 1,
					chancePercent = 100,
				),
			),
		)

	private fun statStageOperationSkill(
		skillId: Long,
		operation: BattleStatStageOperation,
	): BattleSkillSlot =
		damagingSkill(
			skillId = skillId,
			name = "能力操作边界测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = null,
			affectedByProtect = false,
			statStageOperations = listOf(operation),
		)

	private fun clearTarget(stat: BattleStat, chancePercent: Int = 100): BattleStatStageOperation =
		BattleStatStageOperation(
			kind = BattleStatStageOperationKind.CLEAR,
			stat = stat,
			target = BattleStatStageOperationTarget.TARGET,
			chancePercent = chancePercent,
		)

	private fun copyTargetToUser(stat: BattleStat): BattleStatStageOperation =
		BattleStatStageOperation(
			kind = BattleStatStageOperationKind.COPY,
			stat = stat,
			target = BattleStatStageOperationTarget.USER,
			source = BattleStatStageOperationTarget.TARGET,
			chancePercent = 100,
		)

	private fun swapUserAndTarget(stat: BattleStat): BattleStatStageOperation =
		BattleStatStageOperation(
			kind = BattleStatStageOperationKind.SWAP,
			stat = stat,
			target = BattleStatStageOperationTarget.USER,
			source = BattleStatStageOperationTarget.TARGET,
			chancePercent = 100,
		)

	private fun invertTarget(stat: BattleStat): BattleStatStageOperation =
		BattleStatStageOperation(
			kind = BattleStatStageOperationKind.INVERT,
			stat = stat,
			target = BattleStatStageOperationTarget.TARGET,
			chancePercent = 100,
		)
}
