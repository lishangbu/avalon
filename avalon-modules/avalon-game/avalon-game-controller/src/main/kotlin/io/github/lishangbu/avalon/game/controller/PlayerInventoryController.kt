package io.github.lishangbu.avalon.game.controller

import io.github.lishangbu.avalon.game.service.player.GrantInventoryItemCommand
import io.github.lishangbu.avalon.game.service.player.PlayerInventoryItemView
import io.github.lishangbu.avalon.game.service.player.PlayerInventoryManagementService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 玩家背包控制器。 */
@RestController
@RequestMapping("/game/inventory")
class PlayerInventoryController(
    private val playerInventoryManagementService: PlayerInventoryManagementService,
) {
    /** 查询玩家背包。 */
    @GetMapping("/items")
    fun listItems(
        @RequestParam playerId: String,
    ): List<PlayerInventoryItemView> = playerInventoryManagementService.listByPlayerId(playerId)

    /** 发放道具。 */
    @PostMapping("/items/grant")
    fun grantItem(
        @RequestBody command: GrantInventoryItemCommand,
    ): PlayerInventoryItemView = playerInventoryManagementService.grant(command)
}
