package io.github.lishangbu.avalon.game.battle.engine.service.memory

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionFactory
import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionService
import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionStore
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionChoice
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionTurnResult

/**
 * BattleSessionService 的内存版实现。
 *
 * 设计意图：
 * - 为当前阶段提供最小可用的会话服务层。
 * - 适用于本地测试、单进程运行和后续 API 层接入前的过渡形态。
 */
class InMemoryBattleSessionService(
    private val sessionFactory: BattleSessionFactory,
    private val sessionStore: BattleSessionStore = InMemoryBattleSessionStore(),
) : BattleSessionService {
    override fun createSession(
        sessionId: String,
        formatId: String,
    ): BattleSessionQuery {
        require(sessionStore.find(sessionId) == null) { "Session '$sessionId' already exists." }
        val session = sessionFactory.create(sessionId, formatId)
        sessionStore.save(sessionId, session)
        return session.query()
    }

    override fun configureSession(
        sessionId: String,
        battleKind: BattleType,
        capturableSideId: String?,
    ): BattleSessionQuery {
        val session = requireSession(sessionId)
        session.configureBattle(battleKind, capturableSideId)
        return session.query()
    }

    override fun startSession(sessionId: String): BattleSessionQuery {
        val session = requireSession(sessionId)
        session.start()
        return session.query()
    }

    override fun registerSide(
        sessionId: String,
        sideId: String,
    ): BattleSessionQuery {
        val session = requireSession(sessionId)
        session.registerSide(sideId)
        return session.query()
    }

    override fun registerUnit(
        sessionId: String,
        sideId: String,
        unit: UnitState,
        active: Boolean,
    ): BattleSessionQuery {
        val session = requireSession(sessionId)
        session.registerUnit(sideId, unit, active)
        return session.query()
    }

    override fun submitChoice(
        sessionId: String,
        choice: BattleSessionChoice,
    ): BattleSessionQuery {
        val session = requireSession(sessionId)
        session.submitChoice(choice)
        return session.query()
    }

    override fun submitChoices(
        sessionId: String,
        choices: List<BattleSessionChoice>,
    ): BattleSessionQuery {
        val session = requireSession(sessionId)
        session.submitChoices(choices)
        return session.query()
    }

    override fun submitReplacementChoice(
        sessionId: String,
        sideId: String,
        incomingUnitId: String,
    ): BattleSessionQuery {
        val session = requireSession(sessionId)
        session.submitReplacementChoice(sideId, incomingUnitId)
        return session.query()
    }

    override fun recordCaptureFailure(
        sessionId: String,
        ballItemId: String,
        targetUnitId: String,
        shakes: Int,
        reason: String,
        finalRate: Double,
    ): BattleSessionQuery {
        val session = requireSession(sessionId)
        session.recordCaptureFailure(ballItemId, targetUnitId, shakes, reason, finalRate)
        return session.query()
    }

    override fun finishSessionByCapture(
        sessionId: String,
        targetUnitId: String,
    ): BattleSessionQuery {
        val session = requireSession(sessionId)
        session.finishByCapture(targetUnitId)
        return session.query()
    }

    override fun markSessionSettled(sessionId: String): BattleSessionQuery {
        val session = requireSession(sessionId)
        session.markSettled()
        return session.query()
    }

    override fun resolveTurn(sessionId: String): BattleSessionTurnResult {
        val session = requireSession(sessionId)
        return session.resolveTurn()
    }

    override fun querySession(sessionId: String): BattleSessionQuery = requireSession(sessionId).query()

    private fun requireSession(sessionId: String): BattleSession =
        requireNotNull(sessionStore.find(sessionId)) {
            "Session '$sessionId' was not found."
        }
}
