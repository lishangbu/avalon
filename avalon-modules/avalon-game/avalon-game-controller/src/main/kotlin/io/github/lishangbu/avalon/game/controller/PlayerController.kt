package io.github.lishangbu.avalon.game.controller

import io.github.lishangbu.avalon.game.service.player.CreatePlayerCommand
import io.github.lishangbu.avalon.game.service.player.PlayerManagementService
import io.github.lishangbu.avalon.game.service.player.PlayerQueryService
import io.github.lishangbu.avalon.game.service.player.PlayerView
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 玩家控制器。 */
@RestController
@RequestMapping("/game/players")
class PlayerController(
    private val playerQueryService: PlayerQueryService,
    private val playerManagementService: PlayerManagementService,
) {
    /** 查询账号下的玩家列表。 */
    @GetMapping
    fun listPlayers(
        @RequestParam userId: String,
    ): List<PlayerView> = playerQueryService.listByUserId(userId)

    /** 创建玩家。 */
    @PostMapping
    fun createPlayer(
        @RequestBody command: CreatePlayerCommand,
    ): PlayerView = playerManagementService.create(command)
}
