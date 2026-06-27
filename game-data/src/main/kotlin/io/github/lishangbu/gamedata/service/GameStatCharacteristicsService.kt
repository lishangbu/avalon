package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameStatCharacteristicsRepository
import io.github.lishangbu.gamedata.dto.GameStatCharacteristicsRequest
import io.github.lishangbu.gamedata.dto.GameStatCharacteristicsResponse
import org.springframework.stereotype.Service

/**
 * 数值项特征 Service。
 */
@Service
class GameStatCharacteristicsService(
	repository: GameStatCharacteristicsRepository,
) : GameDataTableService<GameStatCharacteristicsRequest, GameStatCharacteristicsResponse>(
	repository,
	GameStatCharacteristicsResponse::from,
)
