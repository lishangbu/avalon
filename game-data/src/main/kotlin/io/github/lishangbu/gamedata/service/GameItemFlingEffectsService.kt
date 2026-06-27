package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameItemFlingEffectsRepository
import io.github.lishangbu.gamedata.dto.GameItemFlingEffectsRequest
import io.github.lishangbu.gamedata.dto.GameItemFlingEffectsResponse
import org.springframework.stereotype.Service

/**
 * 道具投掷效果 Service。
 */
@Service
class GameItemFlingEffectsService(
	repository: GameItemFlingEffectsRepository,
) : GameDataTableService<GameItemFlingEffectsRequest, GameItemFlingEffectsResponse>(
	repository,
	GameItemFlingEffectsResponse::from,
)
