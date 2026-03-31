package io.github.lishangbu.avalon.game.battle.engine.session.specification

/**
 * battle session 校验结果。
 *
 * @property satisfied 当前规格是否满足。
 * @property message 当规格不满足时返回的人类可读错误信息。
 */
data class BattleSessionValidationResult(
    val satisfied: Boolean,
    val message: String? = null,
) {
    companion object {
        /**
         * 创建一个通过的校验结果。
         *
         * @return 表示规格已满足的结果对象。
         */
        fun satisfied(): BattleSessionValidationResult = BattleSessionValidationResult(satisfied = true)

        /**
         * 创建一个失败的校验结果。
         *
         * @param message 当前规格不满足时的错误信息。
         * @return 表示规格未满足的结果对象。
         */
        fun rejected(message: String): BattleSessionValidationResult =
            BattleSessionValidationResult(
                satisfied = false,
                message = message,
            )
    }
}
