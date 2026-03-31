package io.github.lishangbu.avalon.game.battle.engine.model

/**
 * battle session 级确定性随机状态。
 *
 * 设计目标：
 *
 * - 让 battle 内部所有随机数都从同一条序列中消费
 * - 让当前随机游标能够跟随 session state 一起导出、恢复
 * - 避免依赖 JVM 全局随机源，确保回放时序列严格一致
 *
 * 当前采用 SplitMix64 作为底层状态推进算法：
 *
 * - 状态体积小，便于持久化
 * - 实现简单，跨平台行为稳定
 * - 每次生成只依赖上一状态，适合导出/恢复
 */
data class BattleRandomState(
    /** 初始种子，便于审计与问题定位。 */
    val seed: Long,
    /** 当前游标状态。 */
    val state: Long = seed,
    /** 已消费的随机值数量。 */
    val generatedValueCount: Long = 0,
) {
    /**
     * 生成 `[0, bound)` 范围内的均匀整数，并推进随机状态。
     */
    fun nextInt(bound: Int): BattleRandomIntResult {
        require(bound > 0) { "bound must be greater than 0." }

        val unsignedBound = bound.toLong()
        val threshold = java.lang.Long.remainderUnsigned(-unsignedBound, unsignedBound)
        var cursor = this

        while (true) {
            val nextLongResult = cursor.nextLong()
            if (java.lang.Long.compareUnsigned(nextLongResult.value, threshold) >= 0) {
                return BattleRandomIntResult(
                    value =
                        java.lang.Long
                            .remainderUnsigned(nextLongResult.value, unsignedBound)
                            .toInt(),
                    nextState = nextLongResult.nextState,
                )
            }
            cursor = nextLongResult.nextState
        }
    }

    private fun nextLong(): BattleRandomLongResult {
        val nextStateValue = state + GAMMA
        var mixed = nextStateValue
        mixed = (mixed xor (mixed ushr 30)) * MIX_CONST_1
        mixed = (mixed xor (mixed ushr 27)) * MIX_CONST_2
        val value = mixed xor (mixed ushr 31)
        return BattleRandomLongResult(
            value = value,
            nextState =
                copy(
                    state = nextStateValue,
                    generatedValueCount = generatedValueCount + 1,
                ),
        )
    }

    companion object {
        /**
         * 依据 battle 标识稳定派生一个初始种子。
         *
         * 这样即使只拿到建局参数重新创建 session，只要 battleId / formatId 相同，
         * 默认初始随机序列也保持一致；而一旦 session state 被导出保存，
         * 后续则以持久化下来的 [state] 为准继续推进。
         */
        fun seeded(
            battleId: String,
            formatId: String,
        ): BattleRandomState = BattleRandomState(seed = stableSeed("$battleId#$formatId"))

        private fun stableSeed(value: String): Long {
            var hash = FNV_OFFSET_BASIS
            value.forEach { ch ->
                hash = (hash xor ch.code.toLong()) * FNV_PRIME
            }
            return if (hash == 0L) FALLBACK_SEED else hash
        }

        private const val GAMMA: Long = -7046029254386353131L
        private const val MIX_CONST_1: Long = -4658895280553007687L
        private const val MIX_CONST_2: Long = -7723592293110705685L
        private const val FNV_OFFSET_BASIS: Long = -3750763034362895579L
        private const val FNV_PRIME: Long = 1099511628211L
        private const val FALLBACK_SEED: Long = -7046029254386353131L
    }
}

data class BattleRandomIntResult(
    val value: Int,
    val nextState: BattleRandomState,
)

private data class BattleRandomLongResult(
    val value: Long,
    val nextState: BattleRandomState,
)
