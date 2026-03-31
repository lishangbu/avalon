package io.github.lishangbu.avalon.game.battle.engine.runtime.apply

import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation

/**
 * mutation 应用器接口。
 *
 * 设计意图：
 * - 负责把结构化 mutation 列表真正落到状态模型上。
 * - 与动作执行器解耦，使“产生变更”和“提交变更”分离。
 */
interface MutationApplier {
    fun apply(
        mutations: List<BattleMutation>,
        context: MutationApplicationContext,
    ): MutationApplicationResult
}
