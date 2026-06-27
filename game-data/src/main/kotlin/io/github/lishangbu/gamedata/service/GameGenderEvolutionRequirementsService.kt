package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameGenderEvolutionRequirementsRepository
import io.github.lishangbu.gamedata.dto.GameGenderEvolutionRequirementsRequest
import io.github.lishangbu.gamedata.dto.GameGenderEvolutionRequirementsResponse
import org.springframework.stereotype.Service

/**
 * 性别进化要求 Service。
 */
@Service
class GameGenderEvolutionRequirementsService(
	repository: GameGenderEvolutionRequirementsRepository,
) : GameDataTableService<GameGenderEvolutionRequirementsRequest, GameGenderEvolutionRequirementsResponse>(
	repository,
	GameGenderEvolutionRequirementsResponse::from,
)
