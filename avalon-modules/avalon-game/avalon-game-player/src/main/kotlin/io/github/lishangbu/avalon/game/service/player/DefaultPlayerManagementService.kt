package io.github.lishangbu.avalon.game.service.player

import io.github.lishangbu.avalon.game.entity.CreatureStorageBox
import io.github.lishangbu.avalon.game.entity.Player
import io.github.lishangbu.avalon.game.repository.CreatureStorageBoxRepository
import io.github.lishangbu.avalon.game.repository.PlayerRepository
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class DefaultPlayerManagementService(
    private val playerRepository: PlayerRepository,
    private val creatureStorageBoxRepository: CreatureStorageBoxRepository,
) : PlayerManagementService {
    @Transactional(rollbackFor = [Exception::class])
    override fun create(command: CreatePlayerCommand): PlayerView {
        val userId = command.userId.toLongOrNull() ?: error("userId must be a valid long value.")
        val nickname = command.nickname.trim()
        require(nickname.isNotEmpty()) { "nickname must not be blank." }

        val now = Instant.now()
        val slotNo =
            (
                playerRepository
                    .findAll()
                    .filter { player -> player.userId == userId }
                    .maxOfOrNull { player -> player.slotNo }
                    ?: 0
            ) + 1

        val saved =
            playerRepository.save(
                Player {
                    this.userId = userId
                    this.slotNo = slotNo
                    this.nickname = nickname
                    avatar = command.avatar?.trim()?.takeIf { value -> value.isNotEmpty() }
                    createdAt = now
                    updatedAt = now
                },
                SaveMode.INSERT_ONLY,
            )

        creatureStorageBoxRepository.save(
            CreatureStorageBox {
                playerId = saved.id
                name = DEFAULT_BOX_NAME
                sortingOrder = 1
                capacity = DEFAULT_BOX_CAPACITY
                createdAt = now
                updatedAt = now
            },
            SaveMode.INSERT_ONLY,
        )

        return PlayerView(
            id = saved.id.toString(),
            userId = saved.userId.toString(),
            slotNo = saved.slotNo,
            nickname = saved.nickname,
            avatar = saved.avatar,
            createdAt = saved.createdAt,
            updatedAt = saved.updatedAt,
        )
    }

    private companion object {
        const val DEFAULT_BOX_NAME: String = "Box 1"
        const val DEFAULT_BOX_CAPACITY: Int = 30
    }
}
