package io.github.lishangbu.avalon.game.service.battle.view

import io.github.lishangbu.avalon.game.service.battle.CreateImportedBattleSessionRequest
import io.github.lishangbu.avalon.game.service.battle.ImportedBattleSideRequest
import io.github.lishangbu.avalon.game.service.unit.BattleMoveImportRequest
import io.github.lishangbu.avalon.game.service.unit.BattleUnitImportRequest
import org.springframework.stereotype.Component

/**
 * battle API 请求组装器。
 *
 * 设计意图：
 * - 把 controller 接收到的前端 request view 映射为 service 内部使用的请求模型。
 * - 让 `DefaultGameBattleService` 不必直接依赖 controller 层的入参结构。
 */
@Component
class GameBattleRequestAssembler {
    /**
     * 把导入建局 API 请求映射为 service 内部请求。
     */
    fun toCreateImportedSessionRequest(request: CreateImportedBattleSessionApiRequest): CreateImportedBattleSessionRequest =
        CreateImportedBattleSessionRequest(
            sessionId = request.sessionId,
            formatId = request.formatId,
            sides = request.sides.map(::toImportedBattleSideRequest),
            battleKind = request.battleKind,
            capturableSideId = request.capturableSideId,
            autoStart = request.autoStart,
        )

    /**
     * 把导入 side API 请求映射为 service 内部请求。
     */
    private fun toImportedBattleSideRequest(request: GameBattleImportedSideApiRequest): ImportedBattleSideRequest =
        ImportedBattleSideRequest(
            sideId = request.sideId,
            units = request.units.map(::toBattleUnitImportRequest),
            activeUnitIds = request.activeUnitIds,
        )

    /**
     * 把导入单位 API 请求映射为 service 内部请求。
     */
    private fun toBattleUnitImportRequest(request: GameBattleImportedUnitApiRequest): BattleUnitImportRequest =
        BattleUnitImportRequest(
            unitId = request.unitId,
            level = request.level,
            creatureId = request.creatureId,
            creatureInternalName = request.creatureInternalName,
            natureId = request.natureId,
            natureInternalName = request.natureInternalName,
            abilityInternalName = request.abilityInternalName,
            itemId = request.itemId,
            moves = request.moves.map(::toBattleMoveImportRequest),
            ivs = request.ivs,
            evs = request.evs,
            currentHp = request.currentHp,
            statusId = request.statusId,
            volatileIds = request.volatileIds,
            conditionIds = request.conditionIds,
            boosts = request.boosts,
            flags = request.flags,
            forceSwitchRequested = request.forceSwitchRequested,
        )

    /**
     * 把导入招式 API 请求映射为 service 内部请求。
     */
    private fun toBattleMoveImportRequest(request: GameBattleImportedMoveApiRequest): BattleMoveImportRequest =
        BattleMoveImportRequest(
            moveId = request.moveId,
            currentPp = request.currentPp,
        )
}
