package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.SwitchPreventionReason
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * 验证现代主系列战斗生命周期和替换流程。
 *
 * 场景类型：战斗开始、濒死胜负裁定、主动替换、强制补位替换和替换限制 fixture。
 * 参考来源类型：公开成熟对战引擎的战斗队列/行动结算实现，以及公开濒死、替换规则说明。
 * 现代回合中，替换阶段先于技能阶段；成员离场会清理能力阶级和部分临时计数，但保留 HP、PP 和主要异常状态；
 * 已无法战斗的成员可通过强制补位替换离场，而仍可战斗但处于休整、蓄力或锁招等状态的成员不能主动替换。
 * 验证重点：事件流顺序必须能解释最终状态；胜负只在一侧没有任何可战斗成员时产生；替换限制不改变上场槽位。
 */
class BattleLifecycleSwitchPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `battle start event records format and side order`() {
		val fixture = publicBattleRuleFixture(
			name = "battle-start-event-records-format-and-side-order",
			inputSummary = "以标准单打初始状态创建战斗。",
			expectedSummary = "事件流从 BattleStarted 开始，记录赛制 code 和双方 sideId 的稳定顺序。",
		)

		val state = engine.start(initialState())
		val started = assertIs<BattleEvent.BattleStarted>(state.events.single())

		fixture.assertNamed("battle-start-event-records-format-and-side-order")
		assertEquals(0, state.turnNumber)
		assertNull(state.result)
		assertEquals("standard-single", started.formatCode)
		assertEquals(listOf("side-a", "side-b"), started.sideIds)
	}

	@Test
	fun `last opponent faint ends battle with winning side`() {
		val fixture = publicBattleRuleFixture(
			name = "last-opponent-faint-ends-battle-with-winning-side",
			inputSummary = "一方最后一名可战斗成员被伤害技能击倒。",
			expectedSummary = "目标濒死后立即产生 BattleEnded，胜方为仍有可战斗成员的一侧。",
		)
		val state = engine.start(
			initialState(
				first = participant("winner", speed = 100),
				second = participant("last-opponent", speed = 50, currentHp = 20),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("winner", skillId = 1, targetActorId = "last-opponent"),
				BattleAction.UseSkill("last-opponent", skillId = 1, targetActorId = "winner"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val ended = assertIs<BattleEvent.BattleEnded>(resolved.events.last())

		fixture.assertNamed("last-opponent-faint-ends-battle-with-winning-side")
		assertEquals("side-a", resolved.result?.winningSideId)
		assertEquals("all-opponents-fainted", resolved.result?.reason)
		assertEquals("side-a", ended.winningSideId)
		assertEquals("all-opponents-fainted", ended.reason)
	}

	@Test
	fun `active faint with reserve does not end battle`() {
		val fixture = publicBattleRuleFixture(
			name = "active-faint-with-reserve-does-not-end-battle",
			inputSummary = "当前上场成员被击倒，但同一侧仍有可战斗后备成员。",
			expectedSummary = "事件流记录濒死，但不产生 BattleEnded，等待后续强制补位替换。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant("active-target", speed = 50, currentHp = 20),
				secondBench = listOf(participant("reserve", speed = 40)),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "active-target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("active-faint-with-reserve-does-not-end-battle")
		assertNull(resolved.result)
		assertEquals(listOf("active-target"), resolved.events.filterIsInstance<BattleEvent.ParticipantFainted>().map { it.actorId })
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.BattleEnded>())
	}

	@Test
	fun `voluntary switch replaces active slot before skill phase`() {
		val fixture = publicBattleRuleFixture(
			name = "voluntary-switch-replaces-active-slot-before-skill-phase",
			inputSummary = "一方主动替换，另一方本回合攻击离场成员原本所在槽位。",
			expectedSummary = "替换事件先于技能使用事件；技能命中同一槽位的新上场成员。",
		)
		val state = engine.start(
			initialState(
				first = participant("starter", speed = 100),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("attacker", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.SwitchParticipant("starter", targetActorId = "reserve"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "starter"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val switchIndex = resolved.events.indexOfFirst { it is BattleEvent.ParticipantSwitched }
		val skillIndex = resolved.events.indexOfFirst { it is BattleEvent.SkillUsed }
		val skill = resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single()

		fixture.assertNamed("voluntary-switch-replaces-active-slot-before-skill-phase")
		assertEquals(true, switchIndex in 0 until skillIndex)
		assertEquals(listOf("reserve"), resolved.sideOf("reserve")?.activeActorIds)
		assertEquals("reserve", skill.targetActorId)
		assertEquals(72, resolved.participant("reserve")?.currentHp)
	}

	@Test
	fun `fainted active switch is marked forced`() {
		val fixture = publicBattleRuleFixture(
			name = "fainted-active-switch-is-marked-forced",
			inputSummary = "当前上场成员已经无法战斗，提交替换到同侧可战斗后备成员。",
			expectedSummary = "引擎允许补位替换，并把 ParticipantSwitched 标记为 forced=true。",
		)
		val state = engine.start(
			initialState(
				first = participant("fainted-active", speed = 100, currentHp = 0),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("observer", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("fainted-active", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)
		val switched = resolved.events.filterIsInstance<BattleEvent.ParticipantSwitched>().single()

		fixture.assertNamed("fainted-active-switch-is-marked-forced")
		assertEquals(true, switched.forced)
		assertEquals("fainted-active", switched.previousActorId)
		assertEquals("reserve", switched.nextActorId)
		assertEquals(listOf("reserve"), resolved.sideOf("reserve")?.activeActorIds)
	}

	@Test
	fun `switch out clears volatile counters and stat stages`() {
		val fixture = publicBattleRuleFixture(
			name = "switch-out-clears-volatile-counters-and-stat-stages",
			inputSummary = "成员带着攻击、速度能力阶级变化和连续保护计数主动离场。",
			expectedSummary = "离场后能力阶级和连续保护计数清零，剧毒递增计数回到初始值。",
		)
		val starter = participant("starter", speed = 100).copy(
			majorStatus = BattleMajorStatus.BAD_POISON,
			statStages = mapOf(BattleStat.ATTACK to 2, BattleStat.SPEED to -1),
			protectionChain = 1,
			badPoisonCounter = 4,
		)
		val state = engine.start(
			initialState(
				first = starter,
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("observer", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("starter", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)
		val switchedOut = resolved.participant("starter")

		fixture.assertNamed("switch-out-clears-volatile-counters-and-stat-stages")
		assertEquals(0, switchedOut?.statStage(BattleStat.ATTACK))
		assertEquals(0, switchedOut?.statStage(BattleStat.SPEED))
		assertEquals(0, switchedOut?.protectionChain)
		assertEquals(1, switchedOut?.badPoisonCounter)
	}

	@Test
	fun `switch out keeps major status hp and pp`() {
		val fixture = publicBattleRuleFixture(
			name = "switch-out-keeps-major-status-hp-and-pp",
			inputSummary = "成员带着主要异常状态、已损失 HP 和已消耗 PP 主动离场。",
			expectedSummary = "离场不会恢复主要异常状态、HP 或 PP。",
		)
		val chippedSkill = damagingSkill().copy(remainingPp = 21, maxPp = 35)
		val starter = participant("starter", speed = 100, currentHp = 66, skill = chippedSkill)
			.copy(majorStatus = BattleMajorStatus.BAD_POISON)
		val state = engine.start(
			initialState(
				first = starter,
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("observer", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("starter", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)
		val switchedOut = resolved.participant("starter")

		fixture.assertNamed("switch-out-keeps-major-status-hp-and-pp")
		assertEquals(BattleMajorStatus.BAD_POISON, switchedOut?.majorStatus)
		assertEquals(66, switchedOut?.currentHp)
		assertEquals(21, switchedOut?.skillSlot(1)?.remainingPp)
	}

	@Test
	fun `recharge prevents voluntary switch`() {
		val fixture = publicBattleRuleFixture(
			name = "recharge-prevents-voluntary-switch",
			inputSummary = "仍可战斗成员处于休整状态时提交主动替换。",
			expectedSummary = "替换被阻止并产生 SwitchPrevented(reason=RECHARGE)，原成员仍留在上场槽位。",
		)
		val state = engine.start(
			initialState(
				first = participant("recharging", speed = 100).copy(rechargeTurnsRemaining = 1),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("observer", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("recharging", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("recharge-prevents-voluntary-switch")
		assertEquals(listOf("recharging"), resolved.sideOf("recharging")?.activeActorIds)
		assertEquals("recharging", resolved.events.filterIsInstance<BattleEvent.SwitchPrevented>().filter { it.reason == SwitchPreventionReason.RECHARGE }.single().actorId)
	}

	@Test
	fun `charging prevents voluntary switch`() {
		val fixture = publicBattleRuleFixture(
			name = "charging-prevents-voluntary-switch",
			inputSummary = "仍可战斗成员处于蓄力状态时提交主动替换。",
			expectedSummary = "替换被阻止并产生 SwitchPrevented(reason=CHARGING)，原成员仍留在上场槽位并继续释放蓄力技能。",
		)
		val state = engine.start(
			initialState(
				first = participant("charging", speed = 100).copy(
					chargingSkillId = 1,
					chargingTargetActorId = "observer",
					chargingTurnsRemaining = 1,
				),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("observer", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("charging", targetActorId = "reserve")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val prevented = resolved.events.filterIsInstance<BattleEvent.SwitchPrevented>().filter { it.reason == SwitchPreventionReason.CHARGING }.single()

		fixture.assertNamed("charging-prevents-voluntary-switch")
		assertEquals(listOf("charging"), resolved.sideOf("charging")?.activeActorIds)
		assertEquals("charging", prevented.actorId)
		assertEquals(1, prevented.skillId)
		assertEquals(listOf("charging"), resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId })
	}

	@Test
	fun `locked move prevents voluntary switch`() {
		val fixture = publicBattleRuleFixture(
			name = "locked-move-prevents-voluntary-switch",
			inputSummary = "仍可战斗成员处于锁招状态时提交主动替换。",
			expectedSummary = "替换被阻止并产生 SwitchPrevented(reason=LOCKED_MOVE)，原成员仍留在上场槽位并继续执行锁定技能。",
		)
		val state = engine.start(
			initialState(
				first = participant("locked", speed = 100).copy(
					lockedMoveSkillId = 1,
					lockedMoveTargetActorId = "observer",
					lockedMoveTurnsRemaining = 1,
				),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("observer", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("locked", targetActorId = "reserve")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val prevented = resolved.events.filterIsInstance<BattleEvent.SwitchPrevented>().filter { it.reason == SwitchPreventionReason.LOCKED_MOVE }.single()

		fixture.assertNamed("locked-move-prevents-voluntary-switch")
		assertEquals(listOf("locked"), resolved.sideOf("locked")?.activeActorIds)
		assertEquals("locked", prevented.actorId)
		assertEquals(1, prevented.skillId)
		assertEquals(listOf("locked"), resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId })
	}
}
