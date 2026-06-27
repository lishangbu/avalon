package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameItemPocketsRepository
import io.github.lishangbu.gamedata.dto.GameItemPocketsRequest
import io.github.lishangbu.gamedata.dto.GameItemPocketsResponse
import org.springframework.stereotype.Service

/**
 * 道具口袋 Service。
 */
@Service
class GameItemPocketsService(
	repository: GameItemPocketsRepository,
) : GameDataTableService<GameItemPocketsRequest, GameItemPocketsResponse>(
	repository,
	GameItemPocketsResponse::from,
)
