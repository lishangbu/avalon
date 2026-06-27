package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSpeciesDetailsRepository
import io.github.lishangbu.gamedata.dto.GameSpeciesDetailsRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesDetailsResponse
import org.springframework.stereotype.Service

/**
 * 种类详情 Service。
 */
@Service
class GameSpeciesDetailsService(
	repository: GameSpeciesDetailsRepository,
) : GameDataTableService<GameSpeciesDetailsRequest, GameSpeciesDetailsResponse>(
	repository,
	GameSpeciesDetailsResponse::from,
)
