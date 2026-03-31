package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * 基于内存 Map 的 `BattleSessionChoiceHandlerRegistry` 实现。
 *
 * @property entries choice 类型到处理器的不可变映射。
 */
class InMemoryBattleSessionChoiceHandlerRegistry(
    handlers: List<BattleSessionChoiceHandler>,
) : BattleSessionChoiceHandlerRegistry {
    private val entries: Map<KClass<out BattleSessionChoice>, BattleSessionChoiceHandler> =
        handlers.associateBy(BattleSessionChoiceHandler::choiceType)

    init {
        require(entries.size == handlers.size) {
            "Duplicate BattleSessionChoiceHandler registrations were found."
        }
    }

    /**
     * 返回与当前 choice 运行时类型匹配的处理器。
     */
    override fun get(choice: BattleSessionChoice): BattleSessionChoiceHandler =
        entries[choice::class]
            ?: error("No BattleSessionChoiceHandler registered for '${choice::class.qualifiedName}'.")

    companion object {
        /**
         * 创建 battle-engine 内置的默认 choice handler registry。
         *
         * @param commandFactory battle session 命令工厂。
         * @return 包含标准 choice 提交处理器的内存注册中心。
         */
        fun createDefault(commandFactory: BattleSessionCommandFactory): InMemoryBattleSessionChoiceHandlerRegistry =
            InMemoryBattleSessionChoiceHandlerRegistry(
                handlers =
                    listOf(
                        BattleSessionMoveChoiceHandler(commandFactory),
                        BattleSessionSwitchChoiceHandler(commandFactory),
                        BattleSessionItemChoiceHandler(commandFactory),
                        BattleSessionCaptureChoiceHandler(commandFactory),
                        BattleSessionRunChoiceHandler(commandFactory),
                        BattleSessionWaitChoiceHandler(commandFactory),
                    ),
            )
    }
}
