package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `BattleSessionMoveAction` 执行处理器。
 *
 * @property actionExecutionSupport move/item 等 effect 执行辅助组件。
 */
class BattleSessionMoveActionHandler(
    private val actionExecutionSupport: BattleSessionActionExecutionSupport,
) : BattleSessionActionHandler {
    /**
     * 当前处理器负责的 action 类型。
     */
    override val actionType: KClass<out BattleSessionAction> = BattleSessionMoveAction::class

    /**
     * 执行 move action 并返回结算结果。
     */
    override fun execute(
        action: BattleSessionAction,
        session: BattleSession,
    ): BattleSessionActionExecutionResult {
        val moveAction =
            action as? BattleSessionMoveAction
                ?: error("Expected BattleSessionMoveAction but got '${action::class.qualifiedName}'.")
        val finalResult =
            actionExecutionSupport.executeResolvedEffect(
                session = session,
                effectId = moveAction.moveId,
                actorUnitId = moveAction.attackerId,
                targetUnitId = moveAction.targetId,
                accuracy = moveAction.accuracy,
                evasion = moveAction.evasion,
                basePower = moveAction.basePower,
                damage = moveAction.damage,
                attributes = moveAction.attributes,
            )
        return BattleSessionActionExecutionResult(
            action = moveAction,
            snapshot = session.currentSnapshot,
            moveResult = finalResult,
        )
    }
}
