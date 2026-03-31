package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `BattleSessionItemAction` 执行处理器。
 *
 * @property actionExecutionSupport item 结算辅助组件。
 */
class BattleSessionItemActionHandler(
    private val actionExecutionSupport: BattleSessionActionExecutionSupport,
) : BattleSessionActionHandler {
    /**
     * 当前处理器负责的 action 类型。
     */
    override val actionType: KClass<out BattleSessionAction> = BattleSessionItemAction::class

    /**
     * 执行 item action 并返回结算结果。
     */
    override fun execute(
        action: BattleSessionAction,
        session: BattleSession,
    ): BattleSessionActionExecutionResult {
        val itemAction =
            action as? BattleSessionItemAction
                ?: error("Expected BattleSessionItemAction but got '${action::class.qualifiedName}'.")
        val finalResult =
            actionExecutionSupport.executeResolvedEffect(
                session = session,
                effectId = itemAction.itemId,
                actorUnitId = itemAction.actorUnitId,
                targetUnitId = itemAction.targetId,
                accuracy = null,
                evasion = null,
                basePower = 0,
                damage = 0,
                attributes = itemAction.attributes,
            )
        session.recordLog("Executed item ${itemAction.itemId} from ${itemAction.actorUnitId} to ${itemAction.targetId}.")
        session.recordEvent(
            BattleSessionItemExecutedPayload(
                itemId = itemAction.itemId,
                actorUnitId = itemAction.actorUnitId,
                targetId = itemAction.targetId,
            ),
        )
        return BattleSessionActionExecutionResult(
            action = itemAction,
            snapshot = session.currentSnapshot,
            moveResult = finalResult,
        )
    }
}
