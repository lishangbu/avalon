package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState

/**
 * 默认替补策略。
 *
 * 设计意图：
 * - 维持当前最小 session 的行为不变。
 * - 当 active 单位倒下后，按 side.unitIds 顺序选择第一个仍存活的 bench 顶上。
 */
class DefaultReplacementStrategy : ReplacementStrategy {
    override fun selectActiveUnitIds(
        side: SideState,
        units: Map<String, UnitState>,
    ): List<String> {
        val activeIds = side.activeUnitIds
        val faintedActiveIds = activeIds.filter { unitId -> (units[unitId]?.currentHp ?: 0) <= 0 }
        if (faintedActiveIds.isEmpty()) {
            return activeIds
        }

        val remainingBench =
            side.unitIds
                .filterNot { unitId -> unitId in activeIds }
                .filter { unitId -> (units[unitId]?.currentHp ?: 0) > 0 }
                .toMutableList()

        val rebuiltActive = activeIds.toMutableList()
        faintedActiveIds.forEach { faintedId ->
            val index = rebuiltActive.indexOf(faintedId)
            if (index >= 0) {
                rebuiltActive.removeAt(index)
                if (remainingBench.isNotEmpty()) {
                    rebuiltActive.add(index, remainingBench.removeAt(0))
                }
            }
        }
        return rebuiltActive
    }
}
