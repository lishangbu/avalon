package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameHabitatRepository
import io.github.lishangbu.gamedata.dto.GameHabitatRequest
import io.github.lishangbu.gamedata.dto.GameHabitatResponse
import org.springframework.stereotype.Service

/**
 * 栖息地 Service。
 */
@Service
class GameHabitatService(
	repository: GameHabitatRepository,
) : GameDataTableService<GameHabitatRequest, GameHabitatResponse>(
	repository,
	GameHabitatResponse::from,
)
