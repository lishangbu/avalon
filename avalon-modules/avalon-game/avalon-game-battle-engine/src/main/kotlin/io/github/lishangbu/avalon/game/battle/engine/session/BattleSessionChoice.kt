package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 的统一外部输入模型。
 *
 * 设计意图：
 * - 作为 session 面向调用方的统一输入 DTO 接口。
 * - 让 UI、CLI、网络协议层都先组装 choice，再交给 session 处理。
 *
 * 该接口只描述“玩家想做什么”，不直接参与队列执行。
 *
 * @property kind 当前 choice 的命令种类。
 * @property priority 当前 choice 的行动优先级。
 * @property speed 当前 choice 的排序速度值。
 */
interface BattleSessionChoice {
    /**
     * 当前 choice 的命令种类。
     */
    val kind: BattleSessionChoiceKind

    /**
     * 当前 choice 的行动优先级。
     */
    val priority: Int

    /**
     * 当前 choice 的排序速度值。
     */
    val speed: Int
}
