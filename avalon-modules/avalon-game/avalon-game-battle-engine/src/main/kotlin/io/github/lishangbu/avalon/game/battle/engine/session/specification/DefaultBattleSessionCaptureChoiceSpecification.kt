package io.github.lishangbu.avalon.game.battle.engine.session.specification

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession

/**
 * 默认捕捉合法性规格。
 */
class DefaultBattleSessionCaptureChoiceSpecification : BattleSessionCaptureChoiceSpecification {
    /**
     * 校验当前 battle 下是否允许执行捕捉动作。
     */
    override fun validate(
        session: BattleSession,
        playerId: String,
        sourceUnitId: String,
        targetUnitId: String,
    ): BattleSessionValidationResult {
        if (playerId.isBlank()) {
            return BattleSessionValidationResult.rejected("playerId must not be blank.")
        }

        val battle = session.currentSnapshot.battle
        if (battle.battleKind != BattleType.WILD) {
            return BattleSessionValidationResult.rejected("Capture is only allowed in wild battles.")
        }

        val capturableSideId =
            battle.capturableSideId
                ?: return BattleSessionValidationResult.rejected(
                    "Current battle is not configured with a capturable side.",
                )

        val capturableSide =
            session.currentSnapshot.sides[capturableSideId]
                ?: return BattleSessionValidationResult.rejected(
                    "Capturable side '$capturableSideId' was not found.",
                )

        if (targetUnitId !in capturableSide.activeUnitIds) {
            return BattleSessionValidationResult.rejected(
                "Target unit '$targetUnitId' is not currently capturable.",
            )
        }

        if ((session.currentSnapshot.units[targetUnitId]?.currentHp ?: 0) <= 0) {
            return BattleSessionValidationResult.rejected(
                "Target unit '$targetUnitId' is unable to battle.",
            )
        }

        return BattleSessionValidationResult.satisfied()
    }
}
