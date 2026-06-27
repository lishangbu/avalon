package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameEvolutionTriggersRepository
import io.github.lishangbu.gamedata.dto.GameEvolutionTriggersRequest
import io.github.lishangbu.gamedata.dto.GameEvolutionTriggersResponse
import org.springframework.stereotype.Service

/**
 * 进化触发器 Service。
 */
@Service
class GameEvolutionTriggersService(
	repository: GameEvolutionTriggersRepository,
) : GameDataTableService<GameEvolutionTriggersRequest, GameEvolutionTriggersResponse>(
	repository,
	GameEvolutionTriggersResponse::from,
)
