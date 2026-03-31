package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition

/**
 * 单次出招主流程在 pipeline 中共享的可变上下文。
 *
 * 设计意图：
 * - 把 move resolution 的中间状态从 `DefaultBattleFlowEngine` 主体中抽离出来。
 * - 让各个 phase step 只修改自己关注的字段，而不需要关心最终结果对象如何组装。
 *
 * @property moveEffect 当前正在结算的 effect 定义。
 * @property attackerId 出手单位标识。
 * @property targetId 当前目标单位标识。
 * @property sourceId 本次结算的来源单位标识。
 * @property attributes 透传给 hook phase 的附加属性。
 * @property snapshot 当前阶段持有的最新 battle 快照。
 * @property accuracy 当前阶段持有的命中值。
 * @property evasion 当前阶段持有的回避值。
 * @property basePower 当前阶段持有的威力值。
 * @property damage 当前阶段持有的伤害值。
 * @property hitSuccessful 本次出招是否成功命中。
 * @property cancelled 本次出招是否已经在前置阶段被取消。
 */
class BattleMoveResolutionContext(
    snapshot: BattleRuntimeSnapshot,
    val moveEffect: EffectDefinition,
    val attackerId: String,
    val targetId: String,
    val sourceId: String,
    val attributes: Map<String, Any?>,
    accuracy: Int?,
    evasion: Int?,
    basePower: Int,
    damage: Int,
) {
    /**
     * 当前阶段持有的最新 battle 快照。
     */
    var snapshot: BattleRuntimeSnapshot = snapshot

    /**
     * 当前阶段持有的命中值。
     */
    var accuracy: Int? = accuracy

    /**
     * 当前阶段持有的回避值。
     */
    var evasion: Int? = evasion

    /**
     * 当前阶段持有的威力值。
     */
    var basePower: Int = basePower

    /**
     * 当前阶段持有的伤害值。
     */
    var damage: Int = damage

    /**
     * 本次出招是否成功命中。
     */
    var hitSuccessful: Boolean = false

    /**
     * 本次出招是否已经在前置阶段被取消。
     */
    var cancelled: Boolean = false

    /**
     * 把当前上下文标记为已取消。
     *
     * @param nextSnapshot 取消时需要保留的最新快照。
     */
    fun markCancelled(nextSnapshot: BattleRuntimeSnapshot) {
        snapshot = nextSnapshot
        cancelled = true
    }

    /**
     * 把当前上下文收敛为最终的 move resolution 结果。
     *
     * @return 面向调用方的最终结算结果对象。
     */
    fun toResult(): MoveResolutionResult =
        MoveResolutionResult(
            snapshot = snapshot,
            cancelled = cancelled,
            hitSuccessful = hitSuccessful,
            accuracy = accuracy,
            evasion = evasion,
            basePower = basePower,
            damage = damage,
        )
}
