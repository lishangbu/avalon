package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameStatNatureEffectsRepository
import io.github.lishangbu.gamedata.dto.GameStatNatureEffectsRequest
import io.github.lishangbu.gamedata.dto.GameStatNatureEffectsResponse
import org.springframework.stereotype.Service

/**
 * 数值项性格影响 Service。
 */
@Service
class GameStatNatureEffectsService(
	repository: GameStatNatureEffectsRepository,
) : GameDataTableService<GameStatNatureEffectsRequest, GameStatNatureEffectsResponse>(
	repository,
	GameStatNatureEffectsResponse::from,
)
