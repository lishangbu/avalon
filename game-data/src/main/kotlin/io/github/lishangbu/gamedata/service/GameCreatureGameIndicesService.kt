package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCreatureGameIndicesRepository
import io.github.lishangbu.gamedata.dto.GameCreatureGameIndicesRequest
import io.github.lishangbu.gamedata.dto.GameCreatureGameIndicesResponse
import org.springframework.stereotype.Service

/**
 * 生物索引 Service。
 */
@Service
class GameCreatureGameIndicesService(
	repository: GameCreatureGameIndicesRepository,
) : GameDataTableService<GameCreatureGameIndicesRequest, GameCreatureGameIndicesResponse>(
	repository,
	GameCreatureGameIndicesResponse::from,
)
