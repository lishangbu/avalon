package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 结构化事件类型。
 */
enum class BattleSessionEventType {
    SESSION_STARTED,
    SIDE_REGISTERED,
    UNIT_REGISTERED,
    MOVE_QUEUED,
    MOVE_EXECUTED,
    RUN_FAILED,
    CAPTURE_FAILED,
    CAPTURE_SUCCEEDED,
    TURN_RESOLVED,
    TURN_ENDED,
    AUTO_REPLACED,
    BATTLE_ENDED,
}
