package io.github.lishangbu.avalon.game.service.unit

import io.github.lishangbu.avalon.game.battle.engine.model.UnitState

/** 单个招式槽位导入参数。 */
data class BattleMoveImportRequest(
    val moveId: String,
    val currentPp: Int? = null,
)

/** 真实数据智能导入战斗单位时所需的输入。 */
data class BattleUnitImportRequest(
    val unitId: String,
    val level: Int,
    val creatureId: Long? = null,
    val creatureInternalName: String? = null,
    val natureId: Long? = null,
    val natureInternalName: String? = null,
    val abilityInternalName: String? = null,
    val itemId: String? = null,
    val moves: List<BattleMoveImportRequest> = emptyList(),
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

/** 智能导入后的战斗单位结果。 */
data class BattleUnitImportResult(
    val unit: UnitState,
    val creatureId: Long,
    val creatureInternalName: String,
    val creatureName: String,
    val level: Int,
    val requiredExperience: Int,
    val calculatedStats: Map<String, Int>,
)
