package io.github.lishangbu.avalon.game.battle.engine.model

/**
 * 战斗级运行时状态的最小骨架。
 *
 * 设计意图：
 * - 承载整场战斗的全局状态。
 * - 作为事件上下文与快照系统的根节点。
 *
 * 当前版本只保留最核心字段，后续可逐步扩展。
 *
 * @property id 战斗实例唯一标识。
 * @property formatId 当前战斗采用的规则集标识。
 * @property started 战斗是否已经开始。
 * @property turn 当前回合数。
 * @property ended 战斗是否已经结束。
 * @property winner 战斗赢家标识，未结束时可为空。
 */
data class BattleState(
    val id: String,
    val formatId: String,
    val battleKind: BattleType = BattleType.TRAINER,
    val randomState: BattleRandomState = BattleRandomState.seeded(id, formatId),
    val started: Boolean = false,
    val turn: Int = 0,
    val ended: Boolean = false,
    val settled: Boolean = false,
    val winner: String? = null,
    val endedReason: String? = null,
    val capturableSideId: String? = null,
    val capturedUnitId: String? = null,
)
