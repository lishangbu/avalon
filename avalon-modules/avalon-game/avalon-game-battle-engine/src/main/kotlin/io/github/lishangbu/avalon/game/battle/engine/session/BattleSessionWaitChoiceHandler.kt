package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `WaitChoice` 提交处理器。
 *
 * @property commandFactory battle session 命令工厂。
 */
class BattleSessionWaitChoiceHandler(
    private val commandFactory: BattleSessionCommandFactory,
) : BattleSessionChoiceHandler {
    /**
     * 当前处理器负责的 choice 类型。
     */
    override val choiceType: KClass<out BattleSessionChoice> = WaitChoice::class

    /**
     * 把 wait choice 转为可执行的占位动作。
     */
    override fun submit(
        choice: BattleSessionChoice,
        session: BattleSession,
    ): List<BattleSessionAction> {
        val waitChoice = choice as? WaitChoice ?: error("Expected WaitChoice but got '${choice::class.qualifiedName}'.")
        session.ensureStarted()
        session.ensureUnitCanSubmitChoice(waitChoice.unitId)
        session.actionQueue.enqueue(commandFactory.createAction(waitChoice))
        session.recordLog("Queued wait action for unit ${waitChoice.unitId}.")
        session.recordEvent(
            BattleSessionWaitQueuedPayload(
                unitId = waitChoice.unitId,
                priority = waitChoice.priority,
                speed = waitChoice.speed,
            ),
        )
        return session.actionQueue.snapshot()
    }
}
