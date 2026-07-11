package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.model.BattleAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TurnRequirementsTests {
	@Test
	fun `创建后为每个需要人工选择的成员公开可直接提交的行动选项`() {
		val session = BattleSessionRuntime().create(sessionInitialState())

		assertEquals(
			listOf(
				TurnSelectionRequirement(
					actorId = "side-1-actor-1",
					options = listOf(
						BattleAction.UseSkill("side-1-actor-1", 1, "side-1-actor-1"),
						BattleAction.UseSkill("side-1-actor-1", 1, "side-2-actor-1"),
					),
				),
				TurnSelectionRequirement(
					actorId = "side-2-actor-1",
					options = listOf(
						BattleAction.UseSkill("side-2-actor-1", 1, "side-1-actor-1"),
						BattleAction.UseSkill("side-2-actor-1", 1, "side-2-actor-1"),
					),
				),
			),
			session.requirements.selections,
		)
	}

	@Test
	fun `休整行动由 Runtime 自动补齐而不要求客户端伪造选择`() {
		val recharging = sessionParticipant("side-1-actor-1", speed = 100)
			.copy(rechargeTurnsRemaining = 1)
		val runtime = BattleSessionRuntime(
			randomFactory = BattleRandomFactory(::zeroBattleRandom),
		)
		val created = runtime.create(sessionInitialState(first = recharging))
		assertEquals(
			listOf("side-2-actor-1"),
			created.requirements.selections.map { it.actorId },
		)

		val result = runtime.submitTurn(
			created.sessionId,
			TurnCommand(
				commandId = "abcdefab-cdef-4abc-8def-abcdefabcdef",
				expectedRevision = 0,
				actions = listOf(BattleAction.UseSkill("side-2-actor-1", 1, "side-1-actor-1")),
			),
		)

		assertEquals(0, result.session.state.participant("side-1-actor-1")?.rechargeTurnsRemaining)
		assertEquals(
			listOf("side-1-actor-1", "side-2-actor-1"),
			result.turnRecord.submittedActions.map { it.actorId },
		)
	}

	@Test
	fun `struggle fallback is server-derived and does not require a fake skill choice`() {
		val exhaustedFirst = sessionParticipant(
			actorId = "side-1-actor-1",
			speed = 100,
			skill = sessionSkill(remainingPp = 0),
		)
		val exhaustedSecond = sessionParticipant(
			actorId = "side-2-actor-1",
			speed = 80,
			skill = sessionSkill(remainingPp = 0),
		)
		val runtime = BattleSessionRuntime(
			randomFactory = BattleRandomFactory(::zeroBattleRandom),
		)
		val created = runtime.create(sessionInitialState(first = exhaustedFirst, second = exhaustedSecond))

		assertEquals(emptyList(), created.requirements.selections)

		val result = runtime.submitTurn(
			created.sessionId,
			TurnCommand(
				commandId = "12345678-1234-4123-8123-123456789abc",
				expectedRevision = 0,
				actions = emptyList(),
			),
		)

		assertEquals(
			listOf("side-1-actor-1", "side-2-actor-1"),
			result.turnRecord.submittedActions.map { it.actorId },
		)
	}

	@Test
	fun `exhausted actor keeps one canonical struggle choice when switching is available`() {
		val exhausted = sessionParticipant(
			actorId = "side-1-actor-1",
			speed = 100,
			skill = sessionSkill(remainingPp = 0),
		)
		val bench = sessionParticipant("side-1-actor-2", speed = 60)
		val runtime = BattleSessionRuntime(
			randomFactory = BattleRandomFactory(::zeroBattleRandom),
		)
		val created = runtime.create(sessionInitialState(first = exhausted, firstBench = listOf(bench)))

		assertEquals(
			listOf(
				BattleAction.UseSkill("side-1-actor-1", 1, "side-1-actor-1"),
				BattleAction.SwitchParticipant("side-1-actor-1", "side-1-actor-2"),
			),
			created.requirements.selections.single { it.actorId == "side-1-actor-1" }.options,
		)
		assertFailsWith<IncompleteTurnCommandException> {
			runtime.submitTurn(
				created.sessionId,
				TurnCommand(
					commandId = "87654321-4321-4321-8321-cba987654321",
					expectedRevision = 0,
					actions = listOf(BattleAction.UseSkill("side-2-actor-1", 1, "side-1-actor-1")),
				),
			)
		}
	}
}
