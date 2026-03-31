package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

/**
 * 单次出招主流程结算结果。
 *
 * @property snapshot 结算后的 battle 快照。
 * @property cancelled 本次出招是否在前置阶段被取消。
 * @property hitSuccessful 本次出招是否成功命中目标。
 * @property accuracy 经过修正后的命中值。
 * @property evasion 经过修正后的回避值。
 * @property basePower 经过修正后的威力。
 * @property damage 经过修正后的伤害。
 */
data class MoveResolutionResult(
    val snapshot: BattleRuntimeSnapshot,
    val cancelled: Boolean,
    val hitSuccessful: Boolean,
    val accuracy: Int? = null,
    val evasion: Int? = null,
    val basePower: Int,
    val damage: Int,
)
