package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `MoveChoice` 提交处理器。
 *
 * 设计意图：
 * - 负责 move choice 的合法性校验、动作入队与结构化日志记录。
 * - 作为无状态策略组件供 session 或 Spring 容器复用。
 *
 * @property commandFactory battle session 命令工厂。
 */
class BattleSessionMoveChoiceHandler(
    private val commandFactory: BattleSessionCommandFactory,
) : BattleSessionChoiceHandler {
    /**
     * 当前处理器负责的 choice 类型。
     */
    override val choiceType: KClass<out BattleSessionChoice> = MoveChoice::class

    /**
     * 把 move choice 转为可执行的 session action。
     */
    override fun submit(
        choice: BattleSessionChoice,
        session: BattleSession,
    ): List<BattleSessionAction> {
        val moveChoice = choice as? MoveChoice ?: error("Expected MoveChoice but got '${choice::class.qualifiedName}'.")
        session.ensureStarted()
        session.ensureUnitCanSubmitChoice(moveChoice.attackerId)
        session.ensureTargetIsLegalForAction(
            effectId = moveChoice.moveId,
            actorUnitId = moveChoice.attackerId,
            targetUnitId = moveChoice.targetId,
        )
        session.actionQueue.enqueue(commandFactory.createAction(moveChoice))
        session.recordLog("Queued move ${moveChoice.moveId} from ${moveChoice.attackerId} to ${moveChoice.targetId}.")
        session.recordEvent(
            BattleSessionMoveQueuedPayload(
                moveId = moveChoice.moveId,
                attackerId = moveChoice.attackerId,
                targetId = moveChoice.targetId,
                priority = moveChoice.priority,
                speed = moveChoice.speed,
            ),
        )
        return session.actionQueue.snapshot()
    }
}
