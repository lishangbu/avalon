package io.github.lishangbu.avalon.game.battle.engine.runtime.support

import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.MutationApplicationContext
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * mutation 应用阶段的目标选择器解析器。
 *
 * 设计意图：
 * - 在 apply 层把 DSL selector 解析为具体 unit id 集合。
 * - 保持动作执行层与状态提交层之间的职责分离。
 */
object MutationTargetSelectorResolver {
    fun resolve(
        selector: TargetSelectorId,
        context: MutationApplicationContext,
    ): List<String> =
        when (selector) {
            StandardTargetSelectorIds.SELF -> {
                listOfNotNull(context.selfId)
            }

            StandardTargetSelectorIds.TARGET -> {
                listOfNotNull(context.targetId)
            }

            StandardTargetSelectorIds.SOURCE -> {
                listOfNotNull(context.sourceId)
            }

            StandardTargetSelectorIds.ALL -> {
                listOfNotNull(context.selfId, context.targetId, context.sourceId).distinct()
            }

            StandardTargetSelectorIds.SIDE -> {
                context.side?.activeUnitIds.orEmpty()
            }

            StandardTargetSelectorIds.FOE_SIDE -> {
                context.foeSide?.activeUnitIds.orEmpty()
            }

            StandardTargetSelectorIds.ALL_ALLIES -> {
                context.side
                    ?.activeUnitIds
                    .orEmpty()
                    .filterNot { id -> id == context.selfId }
            }

            StandardTargetSelectorIds.ALL_FOES -> {
                context.foeSide?.activeUnitIds.orEmpty()
            }

            StandardTargetSelectorIds.ALLY -> {
                context.side
                    ?.activeUnitIds
                    .orEmpty()
                    .firstOrNull { id -> id != context.selfId }
                    ?.let(::listOf)
                    .orEmpty()
            }

            StandardTargetSelectorIds.FOE -> {
                context.targetId?.let(::listOf) ?: context.foeSide
                    ?.activeUnitIds
                    ?.take(1)
                    .orEmpty()
            }

            StandardTargetSelectorIds.FIELD -> {
                emptyList()
            }

            else -> {
                error("Unsupported target selector '${selector.value}'.")
            }
        }
}
