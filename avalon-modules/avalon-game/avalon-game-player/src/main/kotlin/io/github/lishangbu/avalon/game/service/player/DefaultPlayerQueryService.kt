package io.github.lishangbu.avalon.game.service.player

import io.github.lishangbu.avalon.game.repository.PlayerRepository
import org.springframework.stereotype.Service

@Service
class DefaultPlayerQueryService(
    private val playerRepository: PlayerRepository,
) : PlayerQueryService {
    override fun listByUserId(userId: String): List<PlayerView> {
        val parsedUserId = userId.toLongOrNull() ?: error("userId must be a valid long value.")
        return playerRepository
            .findAll()
            .filter { player -> player.userId == parsedUserId }
            .sortedWith(compareBy<io.github.lishangbu.avalon.game.entity.Player> { player -> player.slotNo }.thenBy { player -> player.id })
            .map { player ->
                PlayerView(
                    id = player.id.toString(),
                    userId = player.userId.toString(),
                    slotNo = player.slotNo,
                    nickname = player.nickname,
                    avatar = player.avatar,
                    createdAt = player.createdAt,
                    updatedAt = player.updatedAt,
                )
            }
    }
}
