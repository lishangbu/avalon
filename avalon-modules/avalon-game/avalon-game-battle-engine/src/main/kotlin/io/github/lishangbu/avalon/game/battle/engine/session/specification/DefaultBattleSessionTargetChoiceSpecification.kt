package io.github.lishangbu.avalon.game.battle.engine.session.specification

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQueryService

/**
 * 默认目标合法性规格。
 *
 * @property targetQueryService battle session 目标查询服务。
 */
class DefaultBattleSessionTargetChoiceSpecification(
    private val targetQueryService: BattleSessionTargetQueryService,
) : BattleSessionTargetChoiceSpecification {
    /**
     * 校验当前 effect 对给定目标的指向是否合法。
     */
    override fun validate(
        session: BattleSession,
        effectId: String,
        actorUnitId: String,
        targetUnitId: String,
    ): BattleSessionValidationResult {
        val targetQuery =
            targetQueryService.resolve(
                snapshot = session.currentSnapshot,
                effectId = effectId,
                actorUnitId = actorUnitId,
            )
        val targetMode = targetQuery.mode

        if (targetUnitId !in session.currentSnapshot.units) {
            return BattleSessionValidationResult.rejected("Target unit '$targetUnitId' does not exist.")
        }
        if (!targetQuery.requiresExplicitTarget &&
            targetUnitId !in targetQuery.availableTargetUnitIds &&
            targetQuery.availableTargetUnitIds.isNotEmpty()
        ) {
            return BattleSessionValidationResult.rejected(
                "Effect '$effectId' does not accept explicit target '$targetUnitId' under mode '$targetMode'.",
            )
        }
        if (targetUnitId !in targetQuery.availableTargetUnitIds) {
            return BattleSessionValidationResult.rejected(
                "Effect '$effectId' does not allow target '$targetUnitId' under mode '$targetMode'.",
            )
        }
        return BattleSessionValidationResult.satisfied()
    }
}
