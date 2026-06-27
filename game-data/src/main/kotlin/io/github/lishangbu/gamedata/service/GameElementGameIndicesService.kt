package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameElementGameIndicesRepository
import io.github.lishangbu.gamedata.dto.GameElementGameIndicesRequest
import io.github.lishangbu.gamedata.dto.GameElementGameIndicesResponse
import org.springframework.stereotype.Service

/**
 * 属性索引 Service。
 */
@Service
class GameElementGameIndicesService(
	repository: GameElementGameIndicesRepository,
) : GameDataTableService<GameElementGameIndicesRequest, GameElementGameIndicesResponse>(
	repository,
	GameElementGameIndicesResponse::from,
)
