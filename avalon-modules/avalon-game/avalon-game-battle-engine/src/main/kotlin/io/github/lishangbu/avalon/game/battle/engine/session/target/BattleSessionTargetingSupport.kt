package io.github.lishangbu.avalon.game.battle.engine.session.target

import io.github.lishangbu.avalon.game.battle.engine.model.SideState

/**
 * BattleSession 目标模型辅助组件。
 *
 * 设计意图：
 * - 把 `data.target` 到内部目标模式的映射集中管理。
 * - 提供当前会话快照下的可选目标计算逻辑。
 */
object BattleSessionTargetingSupport {
    fun resolveMode(rawTarget: String?): BattleSessionTargetMode =
        when (rawTarget) {
            null -> BattleSessionTargetMode.UNKNOWN
            "self", "user" -> BattleSessionTargetMode.SELF
            "ally" -> BattleSessionTargetMode.ALLY
            "selected-pokemon", "random-opponent" -> BattleSessionTargetMode.FOE
            "all-opponents" -> BattleSessionTargetMode.ALL_FOES
            "all-allies", "user-and-allies", "user-or-ally" -> BattleSessionTargetMode.ALL_ALLIES
            "all-other-pokemon" -> BattleSessionTargetMode.ALL_OTHER_POKEMON
            "all-pokemon", "entire-field" -> BattleSessionTargetMode.ALL_POKEMON
            "users-field" -> BattleSessionTargetMode.SIDE
            "opponents-field" -> BattleSessionTargetMode.FOE_SIDE
            else -> BattleSessionTargetMode.UNKNOWN
        }

    fun availableTargetUnitIds(
        mode: BattleSessionTargetMode,
        actorUnitId: String,
        sides: Collection<SideState>,
    ): List<String> {
        val actorSide =
            requireNotNull(sides.firstOrNull { side -> actorUnitId in side.activeUnitIds }) {
                "Actor unit '$actorUnitId' is not currently active."
            }
        val foeSides = sides.filterNot { side -> side.id == actorSide.id }
        val foeActive = foeSides.flatMap(SideState::activeUnitIds)
        val allActive = sides.flatMap(SideState::activeUnitIds)

        return when (mode) {
            BattleSessionTargetMode.SELF -> listOf(actorUnitId)
            BattleSessionTargetMode.ALLY -> actorSide.activeUnitIds.filterNot { unitId -> unitId == actorUnitId }
            BattleSessionTargetMode.FOE -> foeActive
            BattleSessionTargetMode.ALL_ALLIES -> actorSide.activeUnitIds
            BattleSessionTargetMode.ALL_FOES -> foeActive
            BattleSessionTargetMode.ALL_OTHER_POKEMON -> allActive.filterNot { unitId -> unitId == actorUnitId }
            BattleSessionTargetMode.ALL_POKEMON -> allActive
            BattleSessionTargetMode.SIDE -> actorSide.activeUnitIds
            BattleSessionTargetMode.FOE_SIDE -> foeActive
            BattleSessionTargetMode.FIELD -> emptyList()
            BattleSessionTargetMode.UNKNOWN -> allActive
        }
    }

    fun requiresExplicitTarget(mode: BattleSessionTargetMode): Boolean =
        when (mode) {
            BattleSessionTargetMode.SELF,
            BattleSessionTargetMode.ALL_ALLIES,
            BattleSessionTargetMode.ALL_FOES,
            BattleSessionTargetMode.ALL_OTHER_POKEMON,
            BattleSessionTargetMode.ALL_POKEMON,
            BattleSessionTargetMode.SIDE,
            BattleSessionTargetMode.FOE_SIDE,
            BattleSessionTargetMode.FIELD,
            -> false

            BattleSessionTargetMode.ALLY,
            BattleSessionTargetMode.FOE,
            BattleSessionTargetMode.UNKNOWN,
            -> true
        }
}
