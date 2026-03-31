package io.github.lishangbu.avalon.game.service.battle.view

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType

/**
 * 面向前端的导入招式请求视图。
 *
 * @property moveId 招式标识。
 * @property currentPp 当前招式剩余 PP。
 */
data class GameBattleImportedMoveApiRequest(
    val moveId: String,
    val currentPp: Int? = null,
)

/**
 * 面向前端的导入单位请求视图。
 *
 * @property unitId 单位标识。
 * @property level 单位等级。
 * @property creatureId 生物标识。
 * @property creatureInternalName 生物内部名称。
 * @property natureId 性格标识。
 * @property natureInternalName 性格内部名称。
 * @property abilityInternalName 特性内部名称。
 * @property itemId 道具标识。
 * @property moves 单位招式列表。
 * @property ivs 单位 IV 表。
 * @property evs 单位 EV 表。
 * @property currentHp 当前生命值。
 * @property statusId 当前主状态标识。
 * @property volatileIds 当前挥发状态列表。
 * @property conditionIds 当前附着条件列表。
 * @property boosts 当前 stage / boost 表。
 * @property flags 当前单位轻量标记表。
 * @property forceSwitchRequested 当前单位是否被标记为强制替换。
 */
data class GameBattleImportedUnitApiRequest(
    val unitId: String,
    val level: Int,
    val creatureId: Long? = null,
    val creatureInternalName: String? = null,
    val natureId: Long? = null,
    val natureInternalName: String? = null,
    val abilityInternalName: String? = null,
    val itemId: String? = null,
    val moves: List<GameBattleImportedMoveApiRequest> = emptyList(),
    val ivs: Map<String, Int> = emptyMap(),
    val evs: Map<String, Int> = emptyMap(),
    val currentHp: Int? = null,
    val statusId: String? = null,
    val volatileIds: Set<String> = emptySet(),
    val conditionIds: Set<String> = emptySet(),
    val boosts: Map<String, Int> = emptyMap(),
    val flags: Map<String, String> = emptyMap(),
    val forceSwitchRequested: Boolean = false,
)

/**
 * 面向前端的导入 side 请求视图。
 *
 * @property sideId side 标识。
 * @property units side 下的单位请求列表。
 * @property activeUnitIds 当前需要直接上场的单位标识集合。
 */
data class GameBattleImportedSideApiRequest(
    val sideId: String,
    val units: List<GameBattleImportedUnitApiRequest>,
    val activeUnitIds: Set<String> = emptySet(),
)

/**
 * 面向前端的导入建局请求视图。
 *
 * @property sessionId 会话标识。
 * @property formatId 战斗格式标识。
 * @property sides 本次建局的 side 列表。
 * @property battleKind 战斗种类。
 * @property capturableSideId 可被捕捉的 side 标识。
 * @property autoStart 当前会话是否在导入后自动启动。
 */
data class CreateImportedBattleSessionApiRequest(
    val sessionId: String,
    val formatId: String,
    val sides: List<GameBattleImportedSideApiRequest>,
    val battleKind: BattleType = BattleType.TRAINER,
    val capturableSideId: String? = null,
    val autoStart: Boolean = true,
)
