package io.github.lishangbu.avalon.game.controller

import io.github.lishangbu.avalon.game.service.player.CreatureStorageBoxView
import io.github.lishangbu.avalon.game.service.player.OwnedCreatureQueryService
import io.github.lishangbu.avalon.game.service.player.OwnedCreatureSummaryView
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 玩家持有生物与盒子控制器。 */
@RestController
@RequestMapping("/game/owned-creatures")
class OwnedCreatureController(
    private val ownedCreatureQueryService: OwnedCreatureQueryService,
) {
    /** 查询玩家已拥有的生物。 */
    @GetMapping
    fun listOwnedCreatures(
        @RequestParam playerId: String,
    ): List<OwnedCreatureSummaryView> = ownedCreatureQueryService.listByPlayerId(playerId)

    /** 查询玩家盒子。 */
    @GetMapping("/boxes")
    fun listBoxes(
        @RequestParam playerId: String,
    ): List<CreatureStorageBoxView> = ownedCreatureQueryService.listBoxesByPlayerId(playerId)
}
