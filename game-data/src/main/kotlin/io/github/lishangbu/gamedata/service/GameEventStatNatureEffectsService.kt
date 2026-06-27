package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameEventStatNatureEffectsRepository
import io.github.lishangbu.gamedata.dto.GameEventStatNatureEffectsRequest
import io.github.lishangbu.gamedata.dto.GameEventStatNatureEffectsResponse
import org.springframework.stereotype.Service

/**
 * 活动能力性格影响 Service。
 */
@Service
class GameEventStatNatureEffectsService(
	repository: GameEventStatNatureEffectsRepository,
) : GameDataTableService<GameEventStatNatureEffectsRequest, GameEventStatNatureEffectsResponse>(
	repository,
	GameEventStatNatureEffectsResponse::from,
)
