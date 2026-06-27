package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSpeciesRepository
import io.github.lishangbu.gamedata.dto.GameSpeciesRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesResponse
import org.springframework.stereotype.Service

/**
 * 种类资料 Service。
 */
@Service
class GameSpeciesService(
	repository: GameSpeciesRepository,
) : GameDataTableService<GameSpeciesRequest, GameSpeciesResponse>(
	repository,
	GameSpeciesResponse::from,
)
