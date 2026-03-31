package io.github.lishangbu.avalon.game.service.player

interface PlayerQueryService {
    fun listByUserId(userId: String): List<PlayerView>
}
