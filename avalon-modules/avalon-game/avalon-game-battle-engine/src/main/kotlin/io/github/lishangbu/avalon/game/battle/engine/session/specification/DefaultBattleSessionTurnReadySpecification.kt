package io.github.lishangbu.avalon.game.battle.engine.session.specification

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession

/**
 * 默认回合结算规格。
 *
 * 设计意图：
 * - 集中描述“当前回合何时允许进入 resolve”这一组规则。
 * - 保持无状态，便于作为 Spring 单例 Bean 复用。
 */
class DefaultBattleSessionTurnReadySpecification : BattleSessionTurnReadySpecification {
    /**
     * 校验当前回合是否已经满足最小结算条件。
     */
    override fun validate(session: BattleSession): BattleSessionValidationResult {
        val missingUnitIds = session.pendingChoiceUnitIds()
        if (missingUnitIds.isNotEmpty()) {
            return BattleSessionValidationResult.rejected(
                "Cannot resolve turn because these active units have not submitted actions: $missingUnitIds",
            )
        }
        if (session.replacementRequests.isNotEmpty()) {
            return BattleSessionValidationResult.rejected(
                "Cannot resolve turn while replacement requests are pending: ${session.replacementRequests}",
            )
        }
        return BattleSessionValidationResult.satisfied()
    }
}
