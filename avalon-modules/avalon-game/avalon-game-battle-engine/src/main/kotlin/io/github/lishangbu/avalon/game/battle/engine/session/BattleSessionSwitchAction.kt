package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 中的最小可用替换行动。
 *
 * @property sideId 发起替换的 side 标识。
 * @property outgoingUnitId 当前要下场的 active 单位。
 * @property incomingUnitId 要上场的 bench 单位。
 * @property priority 替换优先级。
 * @property speed 排序所需速度值。
 */
data class BattleSessionSwitchAction(
    override val sideId: String,
    override val outgoingUnitId: String,
    override val incomingUnitId: String,
    override val kind: BattleSessionActionKind = BattleSessionActionKind.SWITCH,
    override val priority: Int = 0,
    override val speed: Int = 0,
) : BattleSessionSwitchingAction
