package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCharacteristicsRepository
import io.github.lishangbu.gamedata.dto.GameCharacteristicsRequest
import io.github.lishangbu.gamedata.dto.GameCharacteristicsResponse
import org.springframework.stereotype.Service

/**
 * 个体特征 Service。
 */
@Service
class GameCharacteristicsService(
	repository: GameCharacteristicsRepository,
) : GameDataTableService<GameCharacteristicsRequest, GameCharacteristicsResponse>(
	repository,
	GameCharacteristicsResponse::from,
)
