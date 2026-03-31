package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionCaptureChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionRunChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionTargetChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionTurnReadySpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionUnitChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionValidationResult

/**
 * 负责所有“当前这个输入是否合法”的校验逻辑。
 *
 * 这样 `BattleSession` 的主文件就不需要同时承担：
 * - 状态推进
 * - 输入收集
 * - 输入合法性判断
 *
 * @property session 当前 battle session。
 * @property turnReadySpecification 回合结算前置规格。
 * @property unitChoiceSpecification active 单位行动提交规格。
 * @property runChoiceSpecification side 逃跑提交规格。
 * @property targetChoiceSpecification effect 目标合法性规格。
 * @property captureChoiceSpecification 捕捉动作合法性规格。
 */
internal class BattleSessionChoiceValidator(
    private val session: BattleSession,
    private val turnReadySpecification: BattleSessionTurnReadySpecification,
    private val unitChoiceSpecification: BattleSessionUnitChoiceSpecification,
    private val runChoiceSpecification: BattleSessionRunChoiceSpecification,
    private val targetChoiceSpecification: BattleSessionTargetChoiceSpecification,
    private val captureChoiceSpecification: BattleSessionCaptureChoiceSpecification,
) {
    /**
     * 确认会话已经开始且尚未结束。
     */
    fun ensureStarted() {
        require(session.currentSnapshot.battle.started) { "Battle session must be started before actions can be executed." }
        require(!session.currentSnapshot.battle.ended) { "Battle session has already ended." }
    }

    /**
     * 确认当前回合已经满足结算条件。
     */
    fun ensureTurnReady() {
        turnReadySpecification.validate(session).requireSatisfied()
    }

    /**
     * 确认某个 active 单位本回合还允许继续提交行动。
     *
     * @param unitId 待提交行动的单位 ID
     */
    fun ensureUnitCanSubmitChoice(unitId: String) {
        unitChoiceSpecification.validate(session, unitId).requireSatisfied()
    }

    /**
     * 确认某个 side 允许提交逃跑选择。
     *
     * @param sideId 待逃跑的 side ID
     */
    fun ensureSideCanSubmitRunChoice(sideId: String) {
        runChoiceSpecification.validate(session, sideId).requireSatisfied()
    }

    /**
     * 确认目标是否符合 effect 的 targeting 规则。
     *
     * @param effectId effect ID
     * @param actorUnitId 发起单位 ID
     * @param targetUnitId 目标单位 ID
     */
    fun ensureTargetIsLegalForAction(
        effectId: String,
        actorUnitId: String,
        targetUnitId: String,
    ) {
        targetChoiceSpecification.validate(session, effectId, actorUnitId, targetUnitId).requireSatisfied()
    }

    /**
     * 确认当前允许提交捕捉动作。
     */
    fun ensureCaptureIsLegal(
        playerId: String,
        sourceUnitId: String,
        targetUnitId: String,
    ) {
        captureChoiceSpecification.validate(session, playerId, sourceUnitId, targetUnitId).requireSatisfied()
        ensureUnitCanSubmitChoice(sourceUnitId)
    }

    /**
     * 确认某条规格校验已经通过。
     *
     * @receiver 需要被转换为 `require` 断言的校验结果。
     */
    private fun BattleSessionValidationResult.requireSatisfied() {
        require(satisfied) {
            message ?: "Battle session validation failed."
        }
    }

    /**
     * 根据动作反推出它属于哪个 side。
     */
    fun submittedSideId(action: BattleSessionAction): String? =
        when (action) {
            is BattleSessionSideAction -> action.sideId
            is BattleSessionSubmittingAction -> sideIdOfUnit(action.submittingUnitId)
            else -> null
        }

    /**
     * 根据单位 ID 反查其所属 side。
     */
    fun sideIdOfUnit(unitId: String): String? =
        session.currentSnapshot.sides.values
            .firstOrNull { side -> unitId in side.unitIds }
            ?.id

    /**
     * 统计某个 side 当前已经由哪些 active 单位提交过行动。
     */
    fun submittedUnitIdsForSide(side: SideState): List<String> {
        val pending = session.pendingActions()
        if (pending.any { action -> action is BattleSessionRunAction && action.sideId == side.id }) {
            return side.activeUnitIds
        }
        return pending
            .filterIsInstance<BattleSessionSubmittingAction>()
            .map(BattleSessionSubmittingAction::submittingUnitId)
            .filter { unitId -> unitId in side.activeUnitIds }
    }
}
