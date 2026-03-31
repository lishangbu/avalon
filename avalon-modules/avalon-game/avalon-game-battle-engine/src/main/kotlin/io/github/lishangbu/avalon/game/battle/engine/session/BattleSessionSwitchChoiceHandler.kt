package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `SwitchChoice` 提交处理器。
 *
 * @property commandFactory battle session 命令工厂。
 */
class BattleSessionSwitchChoiceHandler(
    private val commandFactory: BattleSessionCommandFactory,
) : BattleSessionChoiceHandler {
    /**
     * 当前处理器负责的 choice 类型。
     */
    override val choiceType: KClass<out BattleSessionChoice> = SwitchChoice::class

    /**
     * 把 switch choice 转为可执行的替换动作。
     */
    override fun submit(
        choice: BattleSessionChoice,
        session: BattleSession,
    ): List<BattleSessionAction> {
        val switchChoice = choice as? SwitchChoice ?: error("Expected SwitchChoice but got '${choice::class.qualifiedName}'.")
        session.ensureStarted()
        session.ensureUnitCanSubmitChoice(switchChoice.outgoingUnitId)
        session.actionQueue.enqueue(commandFactory.createAction(switchChoice))
        session.recordLog(
            "Queued switch from ${switchChoice.outgoingUnitId} to ${switchChoice.incomingUnitId} for side ${switchChoice.sideId}.",
        )
        session.recordEvent(
            BattleSessionSwitchQueuedPayload(
                sideId = switchChoice.sideId,
                outgoingUnitId = switchChoice.outgoingUnitId,
                incomingUnitId = switchChoice.incomingUnitId,
                priority = switchChoice.priority,
                speed = switchChoice.speed,
            ),
        )
        return session.actionQueue.snapshot()
    }
}
