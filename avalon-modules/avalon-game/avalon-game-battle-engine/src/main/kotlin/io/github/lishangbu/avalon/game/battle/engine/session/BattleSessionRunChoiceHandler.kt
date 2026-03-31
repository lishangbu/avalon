package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `RunChoice` 提交处理器。
 *
 * @property commandFactory battle session 命令工厂。
 */
class BattleSessionRunChoiceHandler(
    private val commandFactory: BattleSessionCommandFactory,
) : BattleSessionChoiceHandler {
    /**
     * 当前处理器负责的 choice 类型。
     */
    override val choiceType: KClass<out BattleSessionChoice> = RunChoice::class

    /**
     * 把 run choice 转为可执行的逃跑动作。
     */
    override fun submit(
        choice: BattleSessionChoice,
        session: BattleSession,
    ): List<BattleSessionAction> {
        val runChoice = choice as? RunChoice ?: error("Expected RunChoice but got '${choice::class.qualifiedName}'.")
        session.ensureStarted()
        session.ensureSideCanSubmitRunChoice(runChoice.sideId)
        session.actionQueue.enqueue(commandFactory.createAction(runChoice))
        session.recordLog("Queued run action for side ${runChoice.sideId}.")
        session.recordEvent(
            BattleSessionRunQueuedPayload(
                sideId = runChoice.sideId,
                priority = runChoice.priority,
                speed = runChoice.speed,
            ),
        )
        return session.actionQueue.snapshot()
    }
}
