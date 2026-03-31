package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState

/**
 * 替补选择策略。
 *
 * 设计意图：
 * - 把“active 单位倒下后由谁顶上”从 BattleSession 主体中拆出。
 * - 允许后续按模式、AI、玩家输入或测试需要替换不同策略。
 */
interface ReplacementStrategy {
    /**
     * 为指定 side 选择下一批 active 单位。
     *
     * @param side 当前 side 状态。
     * @param units 当前全部单位状态表。
     * @return 替换后的 active unit id 列表。
     */
    fun selectActiveUnitIds(
        side: SideState,
        units: Map<String, UnitState>,
    ): List<String>
}
