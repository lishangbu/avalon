package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 中某个 side 的当前回合输入状态。
 *
 * 设计意图：
 * - 把“当前哪些 active 单位已经提交行动、哪些还未提交”显式表达出来。
 * - 让上层调用者在 resolveTurn 之前可以判断回合输入是否完整。
 *
 * @property sideId 当前 side 标识。
 * @property activeUnitIds 当前 side 的 active 单位列表。
 * @property submittedUnitIds 本回合已提交行动的 active 单位列表。
 * @property missingUnitIds 本回合尚未提交行动的 active 单位列表。
 * @property requiredActionCount 当前 side 本回合应提交行动数。
 * @property submittedActionCount 当前 side 本回合已提交行动数。
 * @property ready 当前 side 是否已经满足最小回合提交要求。
 */
data class BattleSessionChoiceStatus(
    val sideId: String,
    val activeUnitIds: List<String>,
    val submittedUnitIds: List<String>,
    val missingUnitIds: List<String>,
    val requiredActionCount: Int,
    val submittedActionCount: Int,
    val ready: Boolean,
)
