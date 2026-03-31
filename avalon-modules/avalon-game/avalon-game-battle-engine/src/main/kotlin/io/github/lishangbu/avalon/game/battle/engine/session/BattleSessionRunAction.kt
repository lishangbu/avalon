package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 中最小可用的逃跑行动。
 *
 * @property sideId 发起逃跑的 side 标识。
 * @property priority 行动优先级。
 * @property speed 排序所需速度值。
 */
data class BattleSessionRunAction(
    override val sideId: String,
    override val kind: BattleSessionActionKind = BattleSessionActionKind.RUN,
    override val priority: Int = 0,
    override val speed: Int = 0,
) : BattleSessionSideAction
