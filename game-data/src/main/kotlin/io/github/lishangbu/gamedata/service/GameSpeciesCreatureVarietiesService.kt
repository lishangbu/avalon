package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSpeciesCreatureVarietiesRepository
import io.github.lishangbu.gamedata.dto.GameSpeciesCreatureVarietiesRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesCreatureVarietiesResponse
import org.springframework.stereotype.Service

/**
 * 种类生物变种 Service。
 */
@Service
class GameSpeciesCreatureVarietiesService(
	repository: GameSpeciesCreatureVarietiesRepository,
) : GameDataTableService<GameSpeciesCreatureVarietiesRequest, GameSpeciesCreatureVarietiesResponse>(
	repository,
	GameSpeciesCreatureVarietiesResponse::from,
)
