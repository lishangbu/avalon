package io.github.lishangbu.avalon.game.battle.engine.model

/**
 * Side 级运行时状态骨架。
 *
 * 设计意图：
 * - 表示一方阵营的局内状态。
 * - 与战斗单位、场地状态解耦，避免把所有字段堆进 BattleState。
 *
 * @property id Side 唯一标识。
 * @property unitIds 当前 side 名下全部单位标识列表。
 * @property activeUnitIds 当前处于 active 槽位的单位标识列表。
 */
data class SideState(
    val id: String,
    val unitIds: List<String> = emptyList(),
    val activeUnitIds: List<String> = emptyList(),
)
