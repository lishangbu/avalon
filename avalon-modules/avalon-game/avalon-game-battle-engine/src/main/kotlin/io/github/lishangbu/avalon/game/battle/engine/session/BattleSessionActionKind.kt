package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle session 执行动作种类。
 *
 * @property sortOrder 当前动作种类在默认排序策略中的顺序权重。
 */
enum class BattleSessionActionKind(
    val sortOrder: Int,
) {
    SWITCH(0),
    ITEM(1),
    CAPTURE(2),
    MOVE(3),
    RUN(4),
    WAIT(5),
}
