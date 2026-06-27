package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameAdvancedContestEffectsRepository
import io.github.lishangbu.gamedata.dto.GameAdvancedContestEffectsRequest
import io.github.lishangbu.gamedata.dto.GameAdvancedContestEffectsResponse
import org.springframework.stereotype.Service

/**
 * 高级评价效果 Service。
 */
@Service
class GameAdvancedContestEffectsService(
	repository: GameAdvancedContestEffectsRepository,
) : GameDataTableService<GameAdvancedContestEffectsRequest, GameAdvancedContestEffectsResponse>(
	repository,
	GameAdvancedContestEffectsResponse::from,
)
