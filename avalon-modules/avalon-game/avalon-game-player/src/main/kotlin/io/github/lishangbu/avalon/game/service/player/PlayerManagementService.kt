package io.github.lishangbu.avalon.game.service.player

interface PlayerManagementService {
    fun create(command: CreatePlayerCommand): PlayerView
}
