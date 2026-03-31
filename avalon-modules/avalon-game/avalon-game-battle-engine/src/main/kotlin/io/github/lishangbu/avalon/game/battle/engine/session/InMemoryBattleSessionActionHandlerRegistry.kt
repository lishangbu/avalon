package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * 基于内存 Map 的 `BattleSessionActionHandlerRegistry` 实现。
 *
 * @property entries action 类型到处理器的不可变映射。
 */
class InMemoryBattleSessionActionHandlerRegistry(
    handlers: List<BattleSessionActionHandler>,
) : BattleSessionActionHandlerRegistry {
    private val entries: Map<KClass<out BattleSessionAction>, BattleSessionActionHandler> =
        handlers.associateBy(BattleSessionActionHandler::actionType)

    init {
        require(entries.size == handlers.size) {
            "Duplicate BattleSessionActionHandler registrations were found."
        }
    }

    /**
     * 返回与当前 action 运行时类型匹配的处理器。
     */
    override fun get(action: BattleSessionAction): BattleSessionActionHandler =
        entries[action::class]
            ?: error("No BattleSessionActionHandler registered for '${action::class.qualifiedName}'.")

    companion object {
        /**
         * 创建 battle-engine 内置的默认 action handler registry。
         *
         * @param actionExecutionSupport action 执行辅助组件。
         * @return 包含标准 action 执行处理器的内存注册中心。
         */
        fun createDefault(actionExecutionSupport: BattleSessionActionExecutionSupport): InMemoryBattleSessionActionHandlerRegistry =
            InMemoryBattleSessionActionHandlerRegistry(
                handlers =
                    listOf(
                        BattleSessionMoveActionHandler(actionExecutionSupport),
                        BattleSessionSwitchActionHandler(actionExecutionSupport),
                        BattleSessionItemActionHandler(actionExecutionSupport),
                        BattleSessionCaptureActionHandler(),
                        BattleSessionRunActionHandler(actionExecutionSupport),
                        BattleSessionWaitActionHandler(),
                    ),
            )
    }
}
