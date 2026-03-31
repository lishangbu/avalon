package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `ItemChoice` 提交处理器。
 *
 * @property commandFactory battle session 命令工厂。
 */
class BattleSessionItemChoiceHandler(
    private val commandFactory: BattleSessionCommandFactory,
) : BattleSessionChoiceHandler {
    /**
     * 当前处理器负责的 choice 类型。
     */
    override val choiceType: KClass<out BattleSessionChoice> = ItemChoice::class

    /**
     * 把 item choice 转为可执行的物品动作。
     */
    override fun submit(
        choice: BattleSessionChoice,
        session: BattleSession,
    ): List<BattleSessionAction> {
        val itemChoice = choice as? ItemChoice ?: error("Expected ItemChoice but got '${choice::class.qualifiedName}'.")
        session.ensureStarted()
        session.ensureUnitCanSubmitChoice(itemChoice.actorUnitId)
        session.ensureTargetIsLegalForAction(
            effectId = itemChoice.itemId,
            actorUnitId = itemChoice.actorUnitId,
            targetUnitId = itemChoice.targetId,
        )
        session.actionQueue.enqueue(commandFactory.createAction(itemChoice))
        session.recordLog("Queued item ${itemChoice.itemId} from ${itemChoice.actorUnitId} to ${itemChoice.targetId}.")
        session.recordEvent(
            BattleSessionItemQueuedPayload(
                itemId = itemChoice.itemId,
                actorUnitId = itemChoice.actorUnitId,
                targetId = itemChoice.targetId,
                priority = itemChoice.priority,
                speed = itemChoice.speed,
            ),
        )
        return session.actionQueue.snapshot()
    }
}
