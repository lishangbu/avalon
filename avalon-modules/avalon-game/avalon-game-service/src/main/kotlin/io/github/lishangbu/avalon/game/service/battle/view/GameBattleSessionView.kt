package io.github.lishangbu.avalon.game.service.battle.view

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType

/**
 * 面向前端的战斗会话视图。
 *
 * @property snapshot 当前战斗快照视图。
 * @property pendingActions 当前待执行动作视图列表。
 * @property choiceStatuses 当前回合各 side 的输入状态视图列表。
 * @property replacementRequests 当前待处理替补请求视图列表。
 * @property resourceLedger 当前资源账本视图列表。
 * @property captureResourceLedger 当前捕捉资源账本视图列表。
 * @property battleLogs 面向前端的人类可读 battle log。
 * @property events 面向前端的结构化事件视图列表。
 */
data class GameBattleSessionView(
    val snapshot: GameBattleSnapshotView,
    val pendingActions: List<GameBattleActionView>,
    val choiceStatuses: List<GameBattleChoiceStatusView>,
    val replacementRequests: List<GameBattleReplacementRequestView>,
    val resourceLedger: List<GameBattleResourceUsageView>,
    val captureResourceLedger: List<GameBattleCaptureResourceUsageView>,
    val battleLogs: List<String>,
    val events: List<GameBattleEventView>,
)

/**
 * 面向前端的战斗快照视图。
 *
 * @property battle 战斗级状态视图。
 * @property field 场地级状态视图。
 * @property units 当前全部单位视图列表。
 * @property sides 当前全部 side 视图列表。
 */
data class GameBattleSnapshotView(
    val battle: GameBattleStateView,
    val field: GameBattleFieldView,
    val units: List<GameBattleUnitView>,
    val sides: List<GameBattleSideView>,
)

/**
 * 面向前端的战斗级状态视图。
 *
 * @property id 战斗实例唯一标识。
 * @property formatId 当前战斗采用的规则集标识。
 * @property battleKind 当前战斗种类。
 * @property started 战斗是否已经开始。
 * @property turn 当前回合数。
 * @property ended 战斗是否已经结束。
 * @property settled 战斗是否已经完成结算。
 * @property winner 战斗赢家标识。
 * @property endedReason 战斗结束原因。
 * @property capturableSideId 可被捕捉的 side 标识。
 * @property capturedUnitId 已被捕捉的目标单位标识。
 */
data class GameBattleStateView(
    val id: String,
    val formatId: String,
    val battleKind: BattleType,
    val started: Boolean,
    val turn: Int,
    val ended: Boolean,
    val settled: Boolean,
    val winner: String?,
    val endedReason: String?,
    val capturableSideId: String?,
    val capturedUnitId: String?,
)

/**
 * 面向前端的场地级状态视图。
 *
 * @property weatherId 当前天气标识。
 * @property terrainId 当前地形标识。
 */
data class GameBattleFieldView(
    val weatherId: String?,
    val terrainId: String?,
)

/**
 * 面向前端的单位状态视图。
 *
 * @property id 单位唯一标识。
 * @property currentHp 当前生命值。
 * @property maxHp 最大生命值。
 * @property statusId 主状态标识。
 * @property abilityId 当前特性标识。
 * @property itemId 当前道具标识。
 * @property typeIds 当前属性列表。
 * @property volatileIds 当前挥发状态列表。
 * @property conditionIds 当前附着条件列表。
 * @property boosts 当前 stage / boost 表。
 * @property stats 当前运行时属性值表。
 * @property movePp 当前招式剩余 PP。
 * @property flags 当前单位轻量标记表。
 * @property forceSwitchRequested 当前单位是否被标记为强制替换。
 */
data class GameBattleUnitView(
    val id: String,
    val currentHp: Int,
    val maxHp: Int,
    val statusId: String?,
    val abilityId: String?,
    val itemId: String?,
    val typeIds: List<String>,
    val volatileIds: List<String>,
    val conditionIds: List<String>,
    val boosts: Map<String, Int>,
    val stats: Map<String, Int>,
    val movePp: Map<String, Int>,
    val flags: Map<String, String>,
    val forceSwitchRequested: Boolean,
)

/**
 * 面向前端的 side 状态视图。
 *
 * @property id side 唯一标识。
 * @property unitIds 当前 side 名下全部单位标识列表。
 * @property activeUnitIds 当前处于 active 槽位的单位标识列表。
 */
data class GameBattleSideView(
    val id: String,
    val unitIds: List<String>,
    val activeUnitIds: List<String>,
)

/**
 * 面向前端的回合输入状态视图。
 *
 * @property sideId 当前 side 标识。
 * @property activeUnitIds 当前 active 单位列表。
 * @property submittedUnitIds 当前已经提交行动的单位列表。
 * @property missingUnitIds 当前尚未提交行动的单位列表。
 * @property requiredActionCount 当前回合要求提交的行动数量。
 * @property submittedActionCount 当前回合已经提交的行动数量。
 * @property ready 当前 side 是否已满足回合结算前置条件。
 */
data class GameBattleChoiceStatusView(
    val sideId: String,
    val activeUnitIds: List<String>,
    val submittedUnitIds: List<String>,
    val missingUnitIds: List<String>,
    val requiredActionCount: Int,
    val submittedActionCount: Int,
    val ready: Boolean,
)

/**
 * 面向前端的替补请求视图。
 *
 * @property sideId 需要替补的 side 标识。
 * @property outgoingUnitIds 当前需要下场的单位列表。
 * @property candidateUnitIds 当前可选替补单位列表。
 */
data class GameBattleReplacementRequestView(
    val sideId: String,
    val outgoingUnitIds: List<String>,
    val candidateUnitIds: List<String>,
)
