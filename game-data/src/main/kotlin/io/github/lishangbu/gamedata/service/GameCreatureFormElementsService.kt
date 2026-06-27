package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCreatureFormElementsRepository
import io.github.lishangbu.gamedata.dto.GameCreatureFormElementsRequest
import io.github.lishangbu.gamedata.dto.GameCreatureFormElementsResponse
import org.springframework.stereotype.Service

/**
 * 生物形态属性 Service。
 */
@Service
class GameCreatureFormElementsService(
	repository: GameCreatureFormElementsRepository,
) : GameDataTableService<GameCreatureFormElementsRequest, GameCreatureFormElementsResponse>(
	repository,
	GameCreatureFormElementsResponse::from,
)
