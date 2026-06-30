package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardApplication
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 入场陷阱规则测试。
 *
 * 这组测试专门覆盖现代主系列中放置在一侧、等待成员换入触发的场上状态。每条公开对照 fixture 都引用成熟公开
 * 实现或公开规则说明，避免把复杂规则只靠本地推导闭门实现。测试文件独立于普通单回合测试，是为了让后续继续
 * 增加入场道具、清除陷阱、魔法反射等规则时，能在同一功能边界内维护。
 */
class BattleEntryHazardTests {
	private val engine = BattleEngine()

	@Test
	fun `entry hazard skill establishes target side and stacks to public maximum`() {
		val fixture = publicBattleRuleFixture(
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

		fixture.assertNamed("spikes-establishes-target-side-and-stacks-to-three-layers")
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
	}

	@Test
	fun `stealth rock entry damage uses rock effectiveness after switch in`() {
		val fixture = publicBattleRuleFixture(
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

		fixture.assertNamed("stealth-rock-damage-uses-rock-effectiveness-after-switch")
		assertTrue(switchIndex in 0 until hazardIndex)
		assertEquals(BattleSideEntryHazardKind.STEALTH_ROCK, hazardEvent.kind)
		assertEquals(25, hazardEvent.amount)
		assertEquals(2.0, hazardEvent.effectiveness)
		assertEquals(75, resolved.participant("fire-reserve")?.currentHp)
	}

	@Test
	fun `spikes damages only grounded switch in participants by layer count`() {
		val fixture = publicBattleRuleFixture(
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

		fixture.assertNamed("spikes-third-layer-damages-grounded-switch-in-only")
		assertEquals(25, grounded.events.filterIsInstance<BattleEvent.EntryHazardDamageApplied>().single().amount)
		assertEquals(75, grounded.participant("reserve")?.currentHp)
		assertEquals(emptyList(), airborne.events.filterIsInstance<BattleEvent.EntryHazardDamageApplied>())
		assertEquals(100, airborne.participant("reserve")?.currentHp)
	}

	@Test
	fun `toxic spikes poisons grounded switch in and poison element absorbs hazard`() {
		val fixture = publicBattleRuleFixture(
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

		fixture.assertNamed("toxic-spikes-two-layers-badly-poisons-and-poison-element-absorbs")
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
		val fixture = publicBattleRuleFixture(
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

		fixture.assertNamed("sticky-web-lowers-grounded-switch-in-speed-stage")
		assertEquals(-1, grounded.participant("reserve")?.statStage(BattleStat.SPEED))
		assertEquals(
			-1,
			grounded.events.filterIsInstance<BattleEvent.EntryHazardStatStageChanged>().single().currentStage,
		)
		assertEquals(0, airborne.participant("reserve")?.statStage(BattleStat.SPEED))
		assertEquals(emptyList(), airborne.events.filterIsInstance<BattleEvent.EntryHazardStatStageChanged>())
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
}
