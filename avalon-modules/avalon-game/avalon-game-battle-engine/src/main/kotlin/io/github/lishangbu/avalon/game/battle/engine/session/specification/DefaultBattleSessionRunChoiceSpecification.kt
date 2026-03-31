package io.github.lishangbu.avalon.game.battle.engine.session.specification

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession

/**
 * 默认逃跑提交规格。
 */
class DefaultBattleSessionRunChoiceSpecification : BattleSessionRunChoiceSpecification {
    /**
     * 校验某个 side 当前是否允许提交逃跑选择。
     */
    override fun validate(
        session: BattleSession,
        sideId: String,
    ): BattleSessionValidationResult {
        val side =
            session.currentSnapshot.sides[sideId]
                ?: return BattleSessionValidationResult.rejected("Side '$sideId' was not found.")

        val submittedUnitIds = session.submittedUnitIdsForSide(side)
        if (submittedUnitIds.isNotEmpty()) {
            return BattleSessionValidationResult.rejected(
                "Side '$sideId' already has submitted action(s) for active units: $submittedUnitIds",
            )
        }
        return BattleSessionValidationResult.satisfied()
    }
}
