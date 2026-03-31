package io.github.lishangbu.avalon.game.service.player

interface OwnedCreatureQueryService {
    fun listByPlayerId(playerId: String): List<OwnedCreatureSummaryView>

    fun listBoxesByPlayerId(playerId: String): List<CreatureStorageBoxView>
}
