package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCreatureHeldItemsRepository
import io.github.lishangbu.gamedata.dto.GameCreatureHeldItemsRequest
import io.github.lishangbu.gamedata.dto.GameCreatureHeldItemsResponse
import org.springframework.stereotype.Service

/**
 * 生物持有道具 Service。
 */
@Service
class GameCreatureHeldItemsService(
	repository: GameCreatureHeldItemsRepository,
) : GameDataTableService<GameCreatureHeldItemsRequest, GameCreatureHeldItemsResponse>(
	repository,
	GameCreatureHeldItemsResponse::from,
)
