package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameLocationsRepository
import io.github.lishangbu.gamedata.dto.GameLocationsRequest
import io.github.lishangbu.gamedata.dto.GameLocationsResponse
import org.springframework.stereotype.Service

/**
 * 地点资料 Service。
 */
@Service
class GameLocationsService(
	repository: GameLocationsRepository,
) : GameDataTableService<GameLocationsRequest, GameLocationsResponse>(
	repository,
	GameLocationsResponse::from,
)
