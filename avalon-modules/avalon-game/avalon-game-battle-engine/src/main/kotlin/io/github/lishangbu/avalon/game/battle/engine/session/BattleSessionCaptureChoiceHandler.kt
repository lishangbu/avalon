package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `CaptureChoice` 提交处理器。
 *
 * @property commandFactory battle session 命令工厂。
 */
class BattleSessionCaptureChoiceHandler(
    private val commandFactory: BattleSessionCommandFactory,
) : BattleSessionChoiceHandler {
    /**
     * 当前处理器负责的 choice 类型。
     */
    override val choiceType: KClass<out BattleSessionChoice> = CaptureChoice::class

    /**
     * 把 capture choice 转为可执行的捕捉动作。
     */
    override fun submit(
        choice: BattleSessionChoice,
        session: BattleSession,
    ): List<BattleSessionAction> {
        val captureChoice = choice as? CaptureChoice ?: error("Expected CaptureChoice but got '${choice::class.qualifiedName}'.")
        session.ensureStarted()
        session.ensureUnitCanSubmitChoice(captureChoice.sourceUnitId)
        session.ensureCaptureIsLegal(
            playerId = captureChoice.playerId,
            sourceUnitId = captureChoice.sourceUnitId,
            targetUnitId = captureChoice.targetId,
        )
        session.actionQueue.enqueue(commandFactory.createAction(captureChoice))
        session.recordLog(
            "Queued capture ${captureChoice.ballItemId} from ${captureChoice.sourceUnitId} to ${captureChoice.targetId} for player ${captureChoice.playerId}.",
        )
        session.recordEvent(
            BattleSessionCaptureQueuedPayload(
                playerId = captureChoice.playerId,
                ballItemId = captureChoice.ballItemId,
                sourceUnitId = captureChoice.sourceUnitId,
                targetId = captureChoice.targetId,
                priority = captureChoice.priority,
                speed = captureChoice.speed,
            ),
        )
        return session.actionQueue.snapshot()
    }
}
