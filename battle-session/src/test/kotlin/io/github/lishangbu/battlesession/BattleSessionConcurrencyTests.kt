package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.BattleEngine
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleSessionConcurrencyTests {
	@Test
	fun `一个 Session 的引擎执行不会阻塞另一个 Session`() {
		val blockingEngine = BlockingSessionEngine()
		val sessionIds = ArrayDeque(
			listOf(
				"11111111-1111-4111-8111-111111111111",
				"22222222-2222-4222-8222-222222222222",
			),
		)
		val runtime = BattleSessionRuntime(
			engine = blockingEngine,
			identifierGenerator = SessionIdentifierGenerator { sessionIds.removeFirst() },
			randomFactory = BattleRandomFactory(::zeroBattleRandom),
		)
		val blocked = runtime.create(sessionInitialState(formatCode = "blocked", maxTurns = 1))
		val free = runtime.create(sessionInitialState(formatCode = "free", maxTurns = 1))
		val executor = Executors.newFixedThreadPool(2)

		try {
			val blockedFuture = executor.submit<TurnCommandResult> {
				runtime.submitTurn(blocked.sessionId, completeCommand("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"))
			}
			assertTrue(blockingEngine.awaitBlocked())

			val freeFuture = executor.submit<TurnCommandResult> {
				runtime.submitTurn(free.sessionId, completeCommand("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"))
			}
			assertEquals(1, freeFuture.get(5, TimeUnit.SECONDS).session.revision)

			blockingEngine.release()
			assertEquals(1, blockedFuture.get(5, TimeUnit.SECONDS).session.revision)
		} finally {
			blockingEngine.release()
			executor.shutdownNow()
		}
	}

	private fun completeCommand(commandId: String): TurnCommand =
		TurnCommand(
			commandId = commandId,
			expectedRevision = 0,
			actions = listOf(
				BattleAction.UseSkill("side-1-actor-1", 1, "side-2-actor-1"),
				BattleAction.UseSkill("side-2-actor-1", 1, "side-1-actor-1"),
			),
		)
}

private class BlockingSessionEngine : BattleSessionEngine {
	private val delegate = BattleEngine()
	private val blocked = CountDownLatch(1)
	private val release = CountDownLatch(1)

	override fun start(initialState: BattleInitialState): BattleState = delegate.start(initialState)

	override fun resolveTurn(
		state: BattleState,
		actions: List<BattleAction>,
		random: BattleRandom,
	): BattleState {
		if (state.format.code == "blocked") {
			blocked.countDown()
			check(release.await(5, TimeUnit.SECONDS)) { "blocked engine was not released" }
		}
		return delegate.resolveTurn(state, actions, random)
	}

	fun awaitBlocked(): Boolean = blocked.await(5, TimeUnit.SECONDS)

	fun release() {
		release.countDown()
	}
}
