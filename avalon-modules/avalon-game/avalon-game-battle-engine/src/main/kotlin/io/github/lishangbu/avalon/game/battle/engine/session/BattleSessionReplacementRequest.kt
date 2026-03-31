package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 中待处理的替补请求。
 *
 * @property sideId 需要选择替补的 side。
 * @property outgoingUnitIds 当前倒下或需离场的 active 单位。
 * @property candidateUnitIds 可替补上场的候选 bench 单位。
 */
data class BattleSessionReplacementRequest(
    val sideId: String,
    val outgoingUnitIds: List<String>,
    val candidateUnitIds: List<String>,
)
