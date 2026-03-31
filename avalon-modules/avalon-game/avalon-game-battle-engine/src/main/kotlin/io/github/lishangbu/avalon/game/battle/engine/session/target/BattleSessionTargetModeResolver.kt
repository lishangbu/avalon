package io.github.lishangbu.avalon.game.battle.engine.session.target

/**
 * effect 目标模式解析器。
 *
 * 设计意图：
 * - 把 effect 定义中的原始 target 表达转换成 session 层使用的目标模式。
 * - 让目标查询与目标合法性规则共享同一套模式解析语义。
 */
interface BattleSessionTargetModeResolver {
    /**
     * 解析指定 effect 的目标模式。
     *
     * @param effectId 被解析的 effect 标识。
     * @return session 层可消费的目标模式。
     */
    fun resolve(effectId: String): BattleSessionTargetMode
}
