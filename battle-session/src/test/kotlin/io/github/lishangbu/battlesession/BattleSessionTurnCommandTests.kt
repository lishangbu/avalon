package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.BattleReplayRecorder
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.random.BattleRandom
import io.github.lishangbu.battlesession.model.BattleSessionStatus
import io.github.lishangbu.battlesession.model.TurnCommand
import io.github.lishangbu.battlesession.runtime.BattleRandomFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** 验证 Turn Command 的完整性、幂等、revision、原子提交与组合行动边界。 */
class BattleSessionTurnCommandTests {
	@Test
	fun `缺少必要选择的回合命令被拒绝且不会推进会话`() {
		val runtime = BattleSessionRuntime()
		val created = runtime.create(sessionInitialState())
		val command = TurnCommand(
			commandId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
			expectedRevision = 0,
			actions = listOf(BattleAction.UseSkill("side-1-actor-1", 1, "side-2-actor-1")),
		)

		assertFailsWith<IncompleteTurnCommandException> {
			runtime.submitTurn(created.sessionId, command)
		}

		val unchanged = runtime.get(created.sessionId)
		assertEquals(0, unchanged.revision)
		assertEquals(0, unchanged.state.turnNumber)
		assertEquals(emptyList(), unchanged.turnRecords)
	}

	@Test
	fun `完整回合命令原子推进并记录服务端随机轨迹`() {
		val runtime = BattleSessionRuntime.createForTesting(
			randomFactory = BattleRandomFactory(::zeroBattleRandom),
		)
		val created = runtime.create(sessionInitialState(maxTurns = 1))
		val command = TurnCommand(
			commandId = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
			expectedRevision = 0,
			actions = listOf(
				BattleAction.UseSkill("side-1-actor-1", 1, "side-2-actor-1"),
				BattleAction.UseSkill("side-2-actor-1", 1, "side-1-actor-1"),
			),
		)

		val result = runtime.submitTurn(created.sessionId, command)

		assertEquals(1, result.session.revision)
		assertEquals(1, result.session.state.turnNumber)
		assertEquals(BattleSessionStatus.COMPLETED, result.session.status)
		assertEquals(command.actions, result.turnRecord.submittedActions)
		assertTrue(result.turnRecord.randomTrace.isNotEmpty())
		assertEquals(result.turnRecord, result.session.turnRecords.single())
		val battleRecord = requireNotNull(result.session.battleRecord)
		assertEquals(result.session.state, BattleReplayRecorder().replay(battleRecord.replay))
	}

	@Test
	fun `相同 commandId 与负载重试返回首次结果且只推进一次`() {
		val runtime = BattleSessionRuntime.createForTesting(
			randomFactory = BattleRandomFactory(::zeroBattleRandom),
		)
		val created = runtime.create(sessionInitialState())
		val command = TurnCommand(
			commandId = "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
			expectedRevision = 0,
			actions = listOf(
				BattleAction.UseSkill("side-1-actor-1", 1, "side-2-actor-1"),
				BattleAction.UseSkill("side-2-actor-1", 1, "side-1-actor-1"),
			),
		)

		val first = runtime.submitTurn(created.sessionId, command)
		val retried = runtime.submitTurn(created.sessionId, command)

		assertEquals(first, retried)
		assertEquals(1, runtime.get(created.sessionId).revision)
		assertEquals(1, runtime.get(created.sessionId).turnRecords.size)
	}

	@Test
	fun `retrying an earlier command returns its original result without rolling back the current session`() {
		val runtime = BattleSessionRuntime.createForTesting(
			randomFactory = BattleRandomFactory(::zeroBattleRandom),
		)
		val created = runtime.create(sessionInitialState())
		val actions = listOf(
			BattleAction.UseSkill("side-1-actor-1", 1, "side-2-actor-1"),
			BattleAction.UseSkill("side-2-actor-1", 1, "side-1-actor-1"),
		)
		val firstCommand = TurnCommand(
			commandId = "12345678-1234-4234-8234-123456789012",
			expectedRevision = 0,
			actions = actions,
		)
		val secondCommand = TurnCommand(
			commandId = "23456789-2345-4345-8345-234567890123",
			expectedRevision = 1,
			actions = actions,
		)

		val first = runtime.submitTurn(created.sessionId, firstCommand)
		val second = runtime.submitTurn(created.sessionId, secondCommand)
		val retried = runtime.submitTurn(created.sessionId, firstCommand)

		assertEquals(first, retried)
		assertEquals(second.session, runtime.get(created.sessionId))
		assertEquals(2, runtime.get(created.sessionId).revision)
	}

	@Test
	fun `新的回合命令使用过期 revision 时被拒绝且不会推进`() {
		val runtime = BattleSessionRuntime.createForTesting(
			randomFactory = BattleRandomFactory(::zeroBattleRandom),
		)
		val created = runtime.create(sessionInitialState())
		val actions = listOf(
			BattleAction.UseSkill("side-1-actor-1", 1, "side-2-actor-1"),
			BattleAction.UseSkill("side-2-actor-1", 1, "side-1-actor-1"),
		)
		runtime.submitTurn(
			created.sessionId,
			TurnCommand("dddddddd-dddd-4ddd-8ddd-dddddddddddd", 0, actions),
		)

		val conflict = assertFailsWith<SessionRevisionConflictException> {
			runtime.submitTurn(
				created.sessionId,
				TurnCommand("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee", 0, actions),
			)
		}

		assertEquals(0, conflict.expectedRevision)
		assertEquals(1, conflict.actualRevision)
		assertEquals(1, runtime.get(created.sessionId).revision)
	}

	@Test
	fun `重复 commandId 携带不同负载时被明确拒绝`() {
		val runtime = BattleSessionRuntime.createForTesting(
			randomFactory = BattleRandomFactory(::zeroBattleRandom),
		)
		val created = runtime.create(sessionInitialState())
		val original = TurnCommand(
			"ffffffff-ffff-4fff-8fff-ffffffffffff",
			0,
			listOf(
				BattleAction.UseSkill("side-1-actor-1", 1, "side-2-actor-1"),
				BattleAction.UseSkill("side-2-actor-1", 1, "side-1-actor-1"),
			),
		)
		runtime.submitTurn(created.sessionId, original)

		assertFailsWith<CommandPayloadConflictException> {
			runtime.submitTurn(
				created.sessionId,
				original.copy(
					actions = listOf(
						BattleAction.UseSkill("side-1-actor-1", 1, "side-1-actor-1"),
						original.actions[1],
					),
				),
			)
		}
		assertEquals(1, runtime.get(created.sessionId).revision)
	}

	@Test
	fun `引擎执行失败不写入状态且同一命令可以安全重试`() {
		val randoms = ArrayDeque<BattleRandom>(
			listOf(
				object : BattleRandom {
					override fun nextInt(bound: Int, reason: String): Int = error("随机源故障")
				},
				zeroBattleRandom(),
			),
		)
		val runtime = BattleSessionRuntime.createForTesting(
			randomFactory = BattleRandomFactory { randoms.removeFirst() },
		)
		val created = runtime.create(sessionInitialState())
		val command = TurnCommand(
			commandId = "13572468-2468-4135-8246-135724681357",
			expectedRevision = 0,
			actions = listOf(
				BattleAction.UseSkill("side-1-actor-1", 1, "side-2-actor-1"),
				BattleAction.UseSkill("side-2-actor-1", 1, "side-1-actor-1"),
			),
		)

		assertFailsWith<IllegalStateException> { runtime.submitTurn(created.sessionId, command) }
		assertEquals(0, runtime.get(created.sessionId).revision)
		assertEquals(emptyList(), runtime.get(created.sessionId).turnRecords)

		assertEquals(1, runtime.submitTurn(created.sessionId, command).session.revision)
	}

	@Test
	fun `two active actors cannot select the same switch target`() {
		val first = sessionParticipant("side-1-actor-1", speed = 100)
		val firstPartner = sessionParticipant("side-1-actor-2", speed = 90)
		val sharedReserve = sessionParticipant("side-1-actor-3", speed = 70)
		val second = sessionParticipant("side-2-actor-1", speed = 80)
		val secondPartner = sessionParticipant("side-2-actor-2", speed = 60)
		val single = sessionInitialState(
			first = first,
			second = second,
			firstBench = listOf(firstPartner, sharedReserve),
			secondBench = listOf(secondPartner),
		)
		val double = single.copy(
			format = single.format.copy(mode = BattleMode.DOUBLE, activeParticipantsPerSide = 2),
			sides = listOf(
				single.sides[0].copy(activeActorIds = listOf(first.actorId, firstPartner.actorId)),
				single.sides[1].copy(activeActorIds = listOf(second.actorId, secondPartner.actorId)),
			),
		)
		val runtime = BattleSessionRuntime.createForTesting(randomFactory = BattleRandomFactory(::zeroBattleRandom))
		val created = runtime.create(double)
		val command = TurnCommand(
			commandId = "24681357-2468-4135-8246-246813572468",
			expectedRevision = 0,
			actions = listOf(
				BattleAction.SwitchParticipant(first.actorId, sharedReserve.actorId),
				BattleAction.SwitchParticipant(firstPartner.actorId, sharedReserve.actorId),
				BattleAction.UseSkill(second.actorId, 1, second.actorId),
				BattleAction.UseSkill(secondPartner.actorId, 1, second.actorId),
			),
		)

		val exception = assertFailsWith<InvalidTurnActionsException> {
			runtime.submitTurn(created.sessionId, command)
		}
		assertEquals(listOf("duplicate-switch-target", "duplicate-switch-target"), exception.violations.map { it.code })
		assertEquals(0, runtime.get(created.sessionId).revision)
	}

	@Test
	fun `two fainted active actors cannot be forced into the same reserve`() {
		val first = sessionParticipant("side-1-actor-1", speed = 100, currentHp = 0)
		val firstPartner = sessionParticipant("side-1-actor-2", speed = 90, currentHp = 0)
		val sharedReserve = sessionParticipant("side-1-actor-3", speed = 70)
		val second = sessionParticipant("side-2-actor-1", speed = 80)
		val secondPartner = sessionParticipant("side-2-actor-2", speed = 60)
		val single = sessionInitialState(
			first = first,
			second = second,
			firstBench = listOf(firstPartner, sharedReserve),
			secondBench = listOf(secondPartner),
		)
		val double = single.copy(
			format = single.format.copy(mode = BattleMode.DOUBLE, activeParticipantsPerSide = 2),
			sides = listOf(
				single.sides[0].copy(activeActorIds = listOf(first.actorId, firstPartner.actorId)),
				single.sides[1].copy(activeActorIds = listOf(second.actorId, secondPartner.actorId)),
			),
		)
		val runtime = BattleSessionRuntime.createForTesting(randomFactory = BattleRandomFactory(::zeroBattleRandom))
		val created = runtime.create(double)
		val command = TurnCommand(
			commandId = "13572468-1357-4135-8246-135724681357",
			expectedRevision = 0,
			actions = listOf(
				BattleAction.SwitchParticipant(first.actorId, sharedReserve.actorId),
				BattleAction.SwitchParticipant(firstPartner.actorId, sharedReserve.actorId),
				BattleAction.UseSkill(second.actorId, 1, second.actorId),
				BattleAction.UseSkill(secondPartner.actorId, 1, second.actorId),
			),
		)

		val exception = assertFailsWith<InvalidTurnActionsException> {
			runtime.submitTurn(created.sessionId, command)
		}
		assertEquals(listOf("duplicate-switch-target", "duplicate-switch-target"), exception.violations.map { it.code })
		assertEquals(0, runtime.get(created.sessionId).revision)
	}
}
