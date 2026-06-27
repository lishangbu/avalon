package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameContestEffectsRepository
import io.github.lishangbu.gamedata.dto.GameContestEffectsRequest
import io.github.lishangbu.gamedata.dto.GameContestEffectsResponse
import org.springframework.stereotype.Service

/**
 * 评价效果 Service。
 */
@Service
class GameContestEffectsService(
	repository: GameContestEffectsRepository,
) : GameDataTableService<GameContestEffectsRequest, GameContestEffectsResponse>(
	repository,
	GameContestEffectsResponse::from,
)
