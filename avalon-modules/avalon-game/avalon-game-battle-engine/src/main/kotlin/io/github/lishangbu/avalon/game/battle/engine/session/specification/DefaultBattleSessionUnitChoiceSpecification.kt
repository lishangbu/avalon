package io.github.lishangbu.avalon.game.battle.engine.session.specification

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession

/**
 * 默认 active 单位行动提交规格。
 */
class DefaultBattleSessionUnitChoiceSpecification : BattleSessionUnitChoiceSpecification {
    /**
     * 校验某个 active 单位当前是否允许继续提交行动。
     */
    override fun validate(
        session: BattleSession,
        unitId: String,
    ): BattleSessionValidationResult {
        val side =
            session.currentSnapshot.sides.values
                .firstOrNull { candidate -> unitId in candidate.activeUnitIds }
                ?: return BattleSessionValidationResult.rejected(
                    "Unit '$unitId' is not currently active and cannot submit a turn action.",
                )

        val submittedUnitIds = session.submittedUnitIdsForSide(side)
        if (unitId in submittedUnitIds) {
            return BattleSessionValidationResult.rejected(
                "Unit '$unitId' has already submitted an action for turn ${session.currentSnapshot.battle.turn}.",
            )
        }
        return BattleSessionValidationResult.satisfied()
    }
}
