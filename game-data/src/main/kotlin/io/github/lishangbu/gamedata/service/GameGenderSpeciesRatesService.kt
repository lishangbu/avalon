package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameGenderSpeciesRatesRepository
import io.github.lishangbu.gamedata.dto.GameGenderSpeciesRatesRequest
import io.github.lishangbu.gamedata.dto.GameGenderSpeciesRatesResponse
import org.springframework.stereotype.Service

/**
 * 性别种类比例 Service。
 */
@Service
class GameGenderSpeciesRatesService(
	repository: GameGenderSpeciesRatesRepository,
) : GameDataTableService<GameGenderSpeciesRatesRequest, GameGenderSpeciesRatesResponse>(
	repository,
	GameGenderSpeciesRatesResponse::from,
)
