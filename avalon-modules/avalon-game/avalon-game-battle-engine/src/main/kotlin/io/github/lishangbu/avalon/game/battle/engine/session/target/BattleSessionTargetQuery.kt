package io.github.lishangbu.avalon.game.battle.engine.session.target

/**
 * BattleSession 对外目标查询结果。
 *
 * @property effectId 被查询的 effect 标识。
 * @property actorUnitId 当前出手单位标识。
 * @property mode 解析后的目标模式。
 * @property availableTargetUnitIds 当前允许选择的单位目标集合。
 * @property requiresExplicitTarget 当前是否要求调用方显式给出目标单位。
 */
data class BattleSessionTargetQuery(
    val effectId: String,
    val actorUnitId: String,
    val mode: BattleSessionTargetMode,
    val availableTargetUnitIds: List<String>,
    val requiresExplicitTarget: Boolean,
)
