package io.github.lishangbu.avalon.game.battle.engine.session.target

/**
 * BattleSession 级别的目标模式。
 *
 * 设计意图：
 * - 把 effect `data.target` 的语义收敛成稳定的内部目标模型。
 * - 让 session 查询和合法性校验都基于同一套模式工作。
 */
enum class BattleSessionTargetMode {
    SELF,
    ALLY,
    FOE,
    ALL_ALLIES,
    ALL_FOES,
    ALL_OTHER_POKEMON,
    ALL_POKEMON,
    SIDE,
    FOE_SIDE,
    FIELD,
    UNKNOWN,
}
