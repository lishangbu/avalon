package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot

/**
 * 管理回合末推进和手动替补提交流程。
 *
 * 它负责会话生命周期中两类“跨多步状态变更”的流程：
 * - 回合结束后的 residual、替补和 turn 递增
 * - 玩家对待处理替补请求提交具体 replacement choice
 */
internal class BattleSessionLifecycleCoordinator(
    private val session: BattleSession,
) {
    /**
     * 提交一个手动替补选择。
     *
     * @param sideId 需要替补的 side
     * @param incomingUnitId 被选中的替补单位
     * @return 更新后的 battle snapshot
     */
    fun submitReplacementChoice(
        sideId: String,
        incomingUnitId: String,
    ): BattleRuntimeSnapshot {
        session.ensureStarted()
        val request =
            requireNotNull(session.replacementRequests.firstOrNull { candidate -> candidate.sideId == sideId }) {
                "No replacement request found for side '$sideId'."
            }
        require(incomingUnitId in request.candidateUnitIds) {
            "Unit '$incomingUnitId' is not a valid replacement candidate for side '$sideId'."
        }
        val side = requireNotNull(session.currentSnapshot.sides[sideId]) { "Side '$sideId' was not found." }
        val nextActiveIds = listOf(incomingUnitId)
        val nextSides = session.currentSnapshot.sides + (sideId to side.copy(activeUnitIds = nextActiveIds))
        session.currentSnapshot = session.currentSnapshot.copy(sides = nextSides)
        session.replacementRequests.removeIf { candidate -> candidate.sideId == sideId }
        session.recordLog("Submitted replacement choice for side $sideId: $incomingUnitId.")
        session.recordEvent(
            BattleSessionAutoReplacedPayload(
                sideId = sideId,
                after = nextActiveIds,
                manual = true,
            ),
        )
        return session.currentSnapshot
    }

    /**
     * 推进到回合结束。
     *
     * 流程顺序固定为：
     * 1. residual phase
     * 2. 濒死/替补处理
     * 3. turn + 1
     */
    fun endTurn(): BattleRuntimeSnapshot {
        session.ensureStarted()
        val residualSnapshot = session.battleFlowEngine.resolveResidualPhase(session.currentSnapshot)
        session.currentSnapshot = session.resolveFaintAndReplacement(residualSnapshot)
        session.currentSnapshot =
            session.currentSnapshot.copy(
                battle = session.currentSnapshot.battle.copy(turn = session.currentSnapshot.battle.turn + 1),
            )
        session.recordLog("Turn ended. Advanced to turn ${session.currentSnapshot.battle.turn}.")
        session.recordEvent(BattleSessionTurnEndedPayload)
        return session.currentSnapshot
    }
}
