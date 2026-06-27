package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameEvolutionDetailsRepository
import io.github.lishangbu.gamedata.dto.GameEvolutionDetailsRequest
import io.github.lishangbu.gamedata.dto.GameEvolutionDetailsResponse
import org.springframework.stereotype.Service

/**
 * 进化条件 Service。
 */
@Service
class GameEvolutionDetailsService(
	repository: GameEvolutionDetailsRepository,
) : GameDataTableService<GameEvolutionDetailsRequest, GameEvolutionDetailsResponse>(
	repository,
	GameEvolutionDetailsResponse::from,
)
