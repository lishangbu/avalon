package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType

/**
 * BattleType 策略扩展点。
 *
 * 设计意图：
 * - 把 battle type 的差异行为从主 service 中拆出。
 * - 统一承载 AI 补动作与结算策略。
 */
internal interface BattleTypePolicy {
    fun supports(battleKind: BattleType): Boolean

    fun settlementHandler(): BattleSettlementHandler
}
