package io.github.lishangbu.avalon.game.battle.engine.runtime.support

/**
 * 比较运算辅助组件。
 */
object ComparisonSupport {
    fun compare(
        left: Double,
        operator: String,
        right: Double,
    ): Boolean =
        when (operator) {
            ">" -> left > right
            ">=" -> left >= right
            "<" -> left < right
            "<=" -> left <= right
            "==" -> left == right
            "!=" -> left != right
            else -> error("Unsupported operator '$operator'.")
        }
}
