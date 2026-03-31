package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 负责濒死替补与胜负结算。
 *
 * 这是 `BattleSession` 里另一段很容易膨胀的逻辑：
 * - 检查 active 单位是否倒下
 * - 计算自动替补
 * - 生成待处理替补请求
 * - 更新最终胜者
 */
internal class BattleSessionReplacementResolver(
    private val session: BattleSession,
) {
    /**
     * 在给定快照上完成“濒死检查 + 自动替补 + 待替补请求 + 胜负更新”。
     *
     * @param snapshot 要处理的快照，默认使用会话当前快照
     * @return 处理后的快照
     */
    fun resolveFaintAndReplacement(snapshot: io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot = session.currentSnapshot): io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot {
        var nextSnapshot = snapshot
        val nextSides = nextSnapshot.sides.toMutableMap()
        session.replacementRequests.clear()

        nextSnapshot.sides.forEach { (sideId, side) ->
            val currentUnits = nextSnapshot.units
            val nextActiveIds = session.replacementStrategy.selectActiveUnitIds(side, currentUnits)
            if (nextActiveIds == side.activeUnitIds) {
                val faintedActiveIds = side.activeUnitIds.filter { unitId -> (currentUnits[unitId]?.currentHp ?: 0) <= 0 }
                if (faintedActiveIds.isNotEmpty()) {
                    val candidates =
                        side.unitIds
                            .filterNot { unitId -> unitId in side.activeUnitIds }
                            .filter { unitId -> (currentUnits[unitId]?.currentHp ?: 0) > 0 }
                    if (candidates.isNotEmpty()) {
                        session.replacementRequests +=
                            BattleSessionReplacementRequest(
                                sideId = sideId,
                                outgoingUnitIds = faintedActiveIds,
                                candidateUnitIds = candidates,
                            )
                    }
                }
                return@forEach
            }
            nextSides[sideId] = side.copy(activeUnitIds = nextActiveIds)
            session.recordLog("Side $sideId auto replaced active units: ${side.activeUnitIds} -> $nextActiveIds.")
            session.recordEvent(
                BattleSessionAutoReplacedPayload(
                    sideId = sideId,
                    before = side.activeUnitIds,
                    after = nextActiveIds,
                ),
            )
        }

        nextSnapshot = nextSnapshot.copy(sides = nextSides)
        return updateWinnerIfNeeded(nextSnapshot)
    }

    /**
     * 根据仍有可战斗单位的 side 计算胜者。
     *
     * @param snapshot 待判定的快照
     * @return 已更新胜者信息的快照
     */
    fun updateWinnerIfNeeded(snapshot: io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot): io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot {
        val survivingSides =
            snapshot.sides.values.filter { side ->
                side.unitIds.any { unitId -> (snapshot.units[unitId]?.currentHp ?: 0) > 0 }
            }
        return when {
            survivingSides.size == 1 -> {
                val winnerId = survivingSides.single().id
                session.recordLog("Battle ended. Winner: $winnerId.")
                session.recordEvent(
                    BattleSessionBattleEndedPayload(winner = winnerId),
                )
                snapshot.copy(
                    battle = snapshot.battle.copy(ended = true, winner = winnerId),
                )
            }

            survivingSides.isEmpty() -> {
                session.recordLog("Battle ended with no surviving sides.")
                session.recordEvent(
                    BattleSessionBattleEndedPayload(winner = null),
                )
                snapshot.copy(
                    battle = snapshot.battle.copy(ended = true, winner = null),
                )
            }

            else -> {
                snapshot
            }
        }
    }
}
