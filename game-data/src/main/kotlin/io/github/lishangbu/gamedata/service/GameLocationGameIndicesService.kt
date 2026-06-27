package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameLocationGameIndicesRepository
import io.github.lishangbu.gamedata.dto.GameLocationGameIndicesRequest
import io.github.lishangbu.gamedata.dto.GameLocationGameIndicesResponse
import org.springframework.stereotype.Service

/**
 * 地点索引 Service。
 */
@Service
class GameLocationGameIndicesService(
	repository: GameLocationGameIndicesRepository,
) : GameDataTableService<GameLocationGameIndicesRequest, GameLocationGameIndicesResponse>(
	repository,
	GameLocationGameIndicesResponse::from,
)
