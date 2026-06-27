package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameEvolutionChainsRepository
import io.github.lishangbu.gamedata.dto.GameEvolutionChainsRequest
import io.github.lishangbu.gamedata.dto.GameEvolutionChainsResponse
import org.springframework.stereotype.Service

/**
 * 进化链 Service。
 */
@Service
class GameEvolutionChainsService(
	repository: GameEvolutionChainsRepository,
) : GameDataTableService<GameEvolutionChainsRequest, GameEvolutionChainsResponse>(
	repository,
	GameEvolutionChainsResponse::from,
)
