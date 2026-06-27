package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameItemGameIndicesRepository
import io.github.lishangbu.gamedata.dto.GameItemGameIndicesRequest
import io.github.lishangbu.gamedata.dto.GameItemGameIndicesResponse
import org.springframework.stereotype.Service

/**
 * 道具索引 Service。
 */
@Service
class GameItemGameIndicesService(
	repository: GameItemGameIndicesRepository,
) : GameDataTableService<GameItemGameIndicesRequest, GameItemGameIndicesResponse>(
	repository,
	GameItemGameIndicesResponse::from,
)
