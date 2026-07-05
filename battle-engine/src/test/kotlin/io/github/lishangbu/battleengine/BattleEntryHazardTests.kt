package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardApplication
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 入场陷阱规则测试。
 *
 * 这组测试专门覆盖现代主系列中放置在一侧、等待成员换入触发的场上状态。每条公开对照 场景 都引用成熟公开
 * 实现或公开规则说明，避免把复杂规则只靠本地推导闭门实现。测试文件独立于普通单回合测试，是为了让后续继续
 * 增加入场道具、清除陷阱、魔法反射等规则时，能在同一功能边界内维护。
 */
class BattleEntryHazardTests {
	private val engine = BattleEngine()

	@Test
	fun `entry hazard skill establishes target side and stacks to public maximum`() {
		val scenario = publicBattleRuleScenario(
			name = "spikes-establishes-target-side-and-stacks-to-three-layers",
			inputSummary = "一名成员连续三次成功使用一侧入场陷阱技能，目标为对手当前上场成员所在侧。",
			expectedSummary = "目标侧陷阱层数依次变为 1、2、3；达到公开规则最大三层后再次使用不继续增加层数。",
		)
		val spikes = entryHazardSkill(
			skillId = 1_901,
			name = "撒菱",
			kind = BattleSideEntryHazardKind.SPIKES,
		)
		val state = engine.start(
			initialState(
				first = participant("setter", speed = 100, skill = spikes),
				second = participant("target", speed = 80),
			),
		)
		val action = listOf(BattleAction.UseSkill("setter", skillId = 1_901, targetActorId = "target"))

		val afterOne = engine.resolveTurn(state, action, ScriptedBattleRandom(emptyList()))
		val afterTwo = engine.resolveTurn(afterOne, action, ScriptedBattleRandom(emptyList()))
		val afterThree = engine.resolveTurn(afterTwo, action, ScriptedBattleRandom(emptyList()))
		val afterFour = engine.resolveTurn(afterThree, action, ScriptedBattleRandom(emptyList()))
		val hazard = afterFour.sides.single { it.sideId == "side-b" }.entryHazards.single()

		scenario.assertNamed("spikes-establishes-target-side-and-stacks-to-three-layers")
		assertEquals(BattleSideEntryHazardKind.SPIKES, hazard.kind)
		assertEquals(3, hazard.layers)
		assertEquals(3, hazard.maxLayers)
		assertEquals(
			listOf(1, 2, 3),
			afterFour.events
				.filterIsInstance<BattleEvent.SideEntryHazardChanged>()
				.filter { it.kind == BattleSideEntryHazardKind.SPIKES }
				.map { it.layers },
		)
		assertEquals(
			"entry-hazard-already-maxed",
			afterFour.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	@Test
	fun `stealth rock entry damage uses rock effectiveness after switch in`() {
		val scenario = publicBattleRuleScenario(
			name = "stealth-rock-damage-uses-rock-effectiveness-after-switch",
			inputSummary = "目标侧已有隐形岩类入场陷阱，火属性后备成员主动换入，规则快照声明岩属性打火属性为 2 倍。",
			expectedSummary = "换入事件先发生，随后按最大 HP * 2 / 8 扣除 25 点伤害，并记录入场陷阱专用伤害事件。",
		)
		val rules = neutralRules().copy(
			elementChart = ElementEffectivenessChart(
				mapOf(6L to mapOf(10L to 2.0)),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("observer", speed = 100),
				second = participant("front", speed = 80),
				secondBench = listOf(participant("fire-reserve", speed = 60, elementId = 10)),
				rules = rules,
				secondSideEntryHazards = listOf(BattleSideEntryHazard(BattleSideEntryHazardKind.STEALTH_ROCK)),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("front", targetActorId = "fire-reserve")),
			ScriptedBattleRandom(emptyList()),
		)
		val hazardEvent = resolved.events.filterIsInstance<BattleEvent.EntryHazardDamageApplied>().single()
		val switchIndex = resolved.events.indexOfFirst { it is BattleEvent.ParticipantSwitched }
		val hazardIndex = resolved.events.indexOfFirst { it is BattleEvent.EntryHazardDamageApplied }

		scenario.assertNamed("stealth-rock-damage-uses-rock-effectiveness-after-switch")
		assertTrue(switchIndex in 0 until hazardIndex)
		assertEquals(BattleSideEntryHazardKind.STEALTH_ROCK, hazardEvent.kind)
		assertEquals(25, hazardEvent.amount)
		assertEquals(2.0, hazardEvent.effectiveness)
		assertEquals(75, resolved.participant("fire-reserve")?.currentHp)
	}

	@Test
	fun `spikes damages only grounded switch in participants by layer count`() {
		val scenario = publicBattleRuleScenario(
			name = "spikes-third-layer-damages-grounded-switch-in-only",
			inputSummary = "目标侧已有三层撒菱类入场陷阱；一个接地成员换入，另一个非接地成员在独立场景换入。",
			expectedSummary = "接地成员受到最大 HP 1/4 的入场伤害，非接地成员不触发撒菱伤害事件。",
		)
		val grounded = switchIntoHazard(
			hazard = BattleSideEntryHazard(BattleSideEntryHazardKind.SPIKES, layers = 3),
			reserveGrounded = true,
		)
		val airborne = switchIntoHazard(
			hazard = BattleSideEntryHazard(BattleSideEntryHazardKind.SPIKES, layers = 3),
			reserveGrounded = false,
		)

		scenario.assertNamed("spikes-third-layer-damages-grounded-switch-in-only")
		assertEquals(25, grounded.events.filterIsInstance<BattleEvent.EntryHazardDamageApplied>().single().amount)
		assertEquals(75, grounded.participant("reserve")?.currentHp)
		assertEquals(emptyList(), airborne.events.filterIsInstance<BattleEvent.EntryHazardDamageApplied>())
		assertEquals(100, airborne.participant("reserve")?.currentHp)
	}

	@Test
	fun `toxic spikes poisons grounded switch in and poison element absorbs hazard`() {
		val scenario = publicBattleRuleScenario(
			name = "toxic-spikes-two-layers-badly-poisons-and-poison-element-absorbs",
			inputSummary = "目标侧已有两层毒菱类入场陷阱；普通接地成员换入，另一个接地毒属性成员在独立场景换入。",
			expectedSummary = "普通接地成员获得剧毒；接地毒属性成员不获得异常状态，并移除该侧毒菱。",
		)
		val poisoned = switchIntoHazard(
			hazard = BattleSideEntryHazard(BattleSideEntryHazardKind.TOXIC_SPIKES, layers = 2),
			reserveElementId = 1,
			reserveGrounded = true,
		)
		val absorbed = switchIntoHazard(
			hazard = BattleSideEntryHazard(BattleSideEntryHazardKind.TOXIC_SPIKES, layers = 2),
			reserveElementId = 4,
			reserveGrounded = true,
		)

		scenario.assertNamed("toxic-spikes-two-layers-badly-poisons-and-poison-element-absorbs")
		assertEquals(BattleMajorStatus.BAD_POISON, poisoned.participant("reserve")?.majorStatus)
		assertEquals(2, poisoned.participant("reserve")?.badPoisonCounter)
		assertEquals(
			BattleMajorStatus.BAD_POISON,
			poisoned.events.filterIsInstance<BattleEvent.EntryHazardStatusApplied>().single().status,
		)
		assertEquals(null, absorbed.participant("reserve")?.majorStatus)
		assertEquals(emptyList(), absorbed.sides.single { it.sideId == "side-b" }.entryHazards)
		assertEquals(
			BattleSideEntryHazardKind.TOXIC_SPIKES,
			absorbed.events.filterIsInstance<BattleEvent.SideEntryHazardRemoved>().single().kind,
		)
	}

	@Test
	fun `sticky web lowers speed stage only for grounded switch in participants`() {
		val scenario = publicBattleRuleScenario(
			name = "sticky-web-lowers-grounded-switch-in-speed-stage",
			inputSummary = "目标侧已有黏黏网类入场陷阱；一个接地成员换入，另一个非接地成员在独立场景换入。",
			expectedSummary = "接地成员速度能力阶级降低 1 级，非接地成员不触发能力阶级变化事件。",
		)
		val grounded = switchIntoHazard(
			hazard = BattleSideEntryHazard(BattleSideEntryHazardKind.STICKY_WEB),
			reserveGrounded = true,
		)
		val airborne = switchIntoHazard(
			hazard = BattleSideEntryHazard(BattleSideEntryHazardKind.STICKY_WEB),
			reserveGrounded = false,
		)

		scenario.assertNamed("sticky-web-lowers-grounded-switch-in-speed-stage")
		assertEquals(-1, grounded.participant("reserve")?.statStage(BattleStat.SPEED))
		assertEquals(
			-1,
			grounded.events.filterIsInstance<BattleEvent.EntryHazardStatStageChanged>().single().currentStage,
		)
		assertEquals(0, airborne.participant("reserve")?.statStage(BattleStat.SPEED))
		assertEquals(emptyList(), airborne.events.filterIsInstance<BattleEvent.EntryHazardStatStageChanged>())
	}

	@Test
	fun `spin cleanup clears user binding leech seed and own side entry hazards after hit`() {
		val scenario = publicBattleRuleScenario(
			name = "spin-cleanup-clears-user-binding-leech-seed-and-own-side-entry-hazards",
			inputSummary = "使用者身上已有束缚和寄生种子，使用者一侧已有四类入场陷阱；清场类伤害技能成功命中目标。",
			expectedSummary = "技能造成伤害后解除使用者束缚和寄生种子，并移除使用者一侧全部入场陷阱；目标侧陷阱不受影响。",
		)
		val cleanupSkill = damagingSkill(
			skillId = 229,
			name = "高速旋转",
			power = 50,
			clearsUserSideHazardsAndTraps = true,
		)
		val user = participant("spinner", speed = 100, skill = cleanupSkill).copy(
			boundByActorId = "immune-target",
			bindingTurnsRemaining = 3,
			leechSeedSourceSideId = "side-b",
			leechSeedSourceActiveIndex = 0,
		)

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = user,
					second = participant("target", speed = 80),
					firstSideEntryHazards = allEntryHazards(),
					secondSideEntryHazards = listOf(BattleSideEntryHazard(BattleSideEntryHazardKind.SPIKES)),
				),
			),
			listOf(BattleAction.UseSkill("spinner", skillId = 229, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val cleanedUser = requireNotNull(resolved.participant("spinner"))
		val bindingCleared = resolved.events.filterIsInstance<BattleEvent.VolatileStatusCleared>().single()
		val removedHazards = resolved.events.filterIsInstance<BattleEvent.SideEntryHazardRemoved>()

		scenario.assertNamed("spin-cleanup-clears-user-binding-leech-seed-and-own-side-entry-hazards")
		assertEquals(0, cleanedUser.bindingTurnsRemaining)
		assertEquals(null, cleanedUser.boundByActorId)
		assertEquals(null, cleanedUser.leechSeedSourceSideId)
		assertEquals(null, cleanedUser.leechSeedSourceActiveIndex)
		assertEquals(BattleVolatileStatus.BINDING, bindingCleared.status)
		assertEquals("spinner", resolved.events.filterIsInstance<BattleEvent.LeechSeedCleared>().single().actorId)
		assertEquals(emptyList(), resolved.sides.single { it.sideId == "side-a" }.entryHazards)
		assertEquals(listOf(BattleSideEntryHazard(BattleSideEntryHazardKind.SPIKES)), resolved.sides.single { it.sideId == "side-b" }.entryHazards)
		assertEquals(
			listOf(
				BattleSideEntryHazardKind.STEALTH_ROCK,
				BattleSideEntryHazardKind.SPIKES,
				BattleSideEntryHazardKind.TOXIC_SPIKES,
				BattleSideEntryHazardKind.STICKY_WEB,
			),
			removedHazards.map { it.kind },
		)
		assertTrue(removedHazards.all { it.actorId == "spinner" && it.sideId == "side-a" && it.skillId == 229L })
	}

	@Test
	fun `spin cleanup does not clear traps when target element is immune`() {
		val scenario = publicBattleRuleScenario(
			name = "spin-cleanup-does-not-clear-when-target-is-immune",
			inputSummary = "使用者一侧已有入场陷阱，清场类伤害技能打向属性免疫目标。",
			expectedSummary = "技能只产生 0 伤害免疫事件并中断成功后效果，不产生清场或寄生解除事件；束缚只按回合末规则自然递减。",
		)
		val cleanupSkill = damagingSkill(
			skillId = 229,
			name = "高速旋转",
			elementId = 1,
			power = 50,
			clearsUserSideHazardsAndTraps = true,
		)
		val rules = neutralRules().copy(
			elementChart = ElementEffectivenessChart(mapOf(1L to mapOf(8L to 0.0))),
		)
		val user = participant("spinner", speed = 100, skill = cleanupSkill).copy(
			boundByActorId = "immune-target",
			bindingTurnsRemaining = 3,
			leechSeedSourceSideId = "side-b",
			leechSeedSourceActiveIndex = 0,
		)

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = user,
					second = participant("immune-target", speed = 80, elementId = 8),
					rules = rules,
					firstSideEntryHazards = allEntryHazards(),
				),
			),
			listOf(BattleAction.UseSkill("spinner", skillId = 229, targetActorId = "immune-target")),
			ScriptedBattleRandom(emptyList()),
		)
		val unchangedUser = requireNotNull(resolved.participant("spinner"))

		scenario.assertNamed("spin-cleanup-does-not-clear-when-target-is-immune")
		assertEquals(0, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(2, unchangedUser.bindingTurnsRemaining)
		assertEquals("immune-target", unchangedUser.boundByActorId)
		assertEquals("side-b", unchangedUser.leechSeedSourceSideId)
		assertEquals(0, unchangedUser.leechSeedSourceActiveIndex)
		assertEquals(allEntryHazards(), resolved.sides.single { it.sideId == "side-a" }.entryHazards)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SideEntryHazardRemoved>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.LeechSeedCleared>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.VolatileStatusCleared>())
	}

	@Test
	fun `tidy up clears both side entry hazards and active substitutes after stat boosts`() {
		val scenario = publicBattleRuleScenario(
			name = "tidy-up-clears-both-side-entry-hazards-and-active-substitutes",
			inputSummary = "双方一侧都有入场陷阱，双方当前上场成员都有替身；使用者成功使用全场清理类变化技能。",
			expectedSummary = "使用者攻击和速度先各提升 1 级，随后双方入场陷阱和当前上场替身全部被清除。",
		)
		val tidyUp = tidyUpSkill()
		val cleaner = participant("cleaner", speed = 100, skill = tidyUp).copy(substituteHp = 25)
		val target = participant("target", speed = 80).copy(substituteHp = 30)

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = cleaner,
					second = target,
					firstSideEntryHazards = allEntryHazards(),
					secondSideEntryHazards = listOf(
						BattleSideEntryHazard(BattleSideEntryHazardKind.STEALTH_ROCK),
						BattleSideEntryHazard(BattleSideEntryHazardKind.STICKY_WEB),
					),
				),
			),
			listOf(BattleAction.UseSkill("cleaner", skillId = 882, targetActorId = "cleaner")),
			ScriptedBattleRandom(emptyList()),
		)
		val cleanerAfter = requireNotNull(resolved.participant("cleaner"))
		val targetAfter = requireNotNull(resolved.participant("target"))
		val removedHazards = resolved.events.filterIsInstance<BattleEvent.SideEntryHazardRemoved>()
		val clearedSubstitutes = resolved.events.filterIsInstance<BattleEvent.SubstituteCleared>()
		val firstStatChangeIndex = resolved.events.indexOfFirst { it is BattleEvent.StatStageChanged }
		val firstHazardRemovalIndex = resolved.events.indexOfFirst { it is BattleEvent.SideEntryHazardRemoved }
		val firstSubstituteRemovalIndex = resolved.events.indexOfFirst { it is BattleEvent.SubstituteCleared }

		scenario.assertNamed("tidy-up-clears-both-side-entry-hazards-and-active-substitutes")
		assertEquals(1, cleanerAfter.statStage(BattleStat.ATTACK))
		assertEquals(1, cleanerAfter.statStage(BattleStat.SPEED))
		assertEquals(0, cleanerAfter.substituteHp)
		assertEquals(0, targetAfter.substituteHp)
		assertEquals(emptyList(), resolved.sides.single { it.sideId == "side-a" }.entryHazards)
		assertEquals(emptyList(), resolved.sides.single { it.sideId == "side-b" }.entryHazards)
		assertEquals(
			listOf(
				"side-a" to BattleSideEntryHazardKind.STEALTH_ROCK,
				"side-a" to BattleSideEntryHazardKind.SPIKES,
				"side-a" to BattleSideEntryHazardKind.TOXIC_SPIKES,
				"side-a" to BattleSideEntryHazardKind.STICKY_WEB,
				"side-b" to BattleSideEntryHazardKind.STEALTH_ROCK,
				"side-b" to BattleSideEntryHazardKind.STICKY_WEB,
			),
			removedHazards.map { it.sideId to it.kind },
		)
		assertEquals(listOf("cleaner", "target"), clearedSubstitutes.map { it.actorId })
		assertTrue(removedHazards.all { it.actorId == "cleaner" && it.skillId == 882L })
		assertTrue(clearedSubstitutes.all { it.skillId == 882L })
		assertTrue(firstStatChangeIndex in 0 until firstHazardRemovalIndex)
		assertTrue(firstHazardRemovalIndex in 0 until firstSubstituteRemovalIndex)
	}

	@Test
	fun `tidy up still boosts user when no hazards or substitutes exist`() {
		val scenario = publicBattleRuleScenario(
			name = "tidy-up-still-boosts-user-without-cleanup-targets",
			inputSummary = "场上没有入场陷阱，也没有任何当前上场替身；使用者成功使用全场清理类变化技能。",
			expectedSummary = "技能不会因为没有可清理状态而失败，使用者攻击和速度仍各提升 1 级。",
		)

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("cleaner", speed = 100, skill = tidyUpSkill()),
					second = participant("target", speed = 80),
				),
			),
			listOf(BattleAction.UseSkill("cleaner", skillId = 882, targetActorId = "cleaner")),
			ScriptedBattleRandom(emptyList()),
		)
		val cleanerAfter = requireNotNull(resolved.participant("cleaner"))

		scenario.assertNamed("tidy-up-still-boosts-user-without-cleanup-targets")
		assertEquals(1, cleanerAfter.statStage(BattleStat.ATTACK))
		assertEquals(1, cleanerAfter.statStage(BattleStat.SPEED))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SideEntryHazardRemoved>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SubstituteCleared>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillFailed>())
	}

	private fun switchIntoHazard(
		hazard: BattleSideEntryHazard,
		reserveElementId: Long = 1,
		reserveGrounded: Boolean = true,
	) =
		engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("observer", speed = 100),
					second = participant("front", speed = 80),
					secondBench = listOf(
						participant(
							actorId = "reserve",
							speed = 60,
							elementId = reserveElementId,
							grounded = reserveGrounded,
						),
					),
					secondSideEntryHazards = listOf(hazard),
				),
			),
			listOf(BattleAction.SwitchParticipant("front", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

	private fun allEntryHazards(): List<BattleSideEntryHazard> =
		listOf(
			BattleSideEntryHazard(BattleSideEntryHazardKind.STEALTH_ROCK),
			BattleSideEntryHazard(BattleSideEntryHazardKind.SPIKES, layers = 3),
			BattleSideEntryHazard(BattleSideEntryHazardKind.TOXIC_SPIKES, layers = 2),
			BattleSideEntryHazard(BattleSideEntryHazardKind.STICKY_WEB),
		)

	private fun entryHazardSkill(
		skillId: Long,
		name: String,
		kind: BattleSideEntryHazardKind,
	): BattleSkillSlot =
		damagingSkill(
			skillId = skillId,
			name = name,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = null,
			affectedByProtect = false,
			sideEntryHazardApplications = listOf(
				BattleSideEntryHazardApplication(
					targetSide = BattleSideConditionTarget.TARGET_SIDE,
					hazard = BattleSideEntryHazard(kind),
				),
			),
		)

	private fun tidyUpSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 882,
			name = "大扫除",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = null,
			targetScope = BattleSkillTargetScope.SELF,
			affectedByProtect = false,
			clearsFieldHazardsAndSubstitutes = true,
			statStageEffects = listOf(
				BattleStatStageEffect(
					target = BattleEffectTarget.USER,
					stat = BattleStat.ATTACK,
					stageDelta = 1,
					chancePercent = 100,
				),
				BattleStatStageEffect(
					target = BattleEffectTarget.USER,
					stat = BattleStat.SPEED,
					stageDelta = 1,
					chancePercent = 100,
				),
			),
		)
}
