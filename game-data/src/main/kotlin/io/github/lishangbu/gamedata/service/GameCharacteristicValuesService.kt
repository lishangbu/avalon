package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCharacteristicValuesRepository
import io.github.lishangbu.gamedata.dto.GameCharacteristicValuesRequest
import io.github.lishangbu.gamedata.dto.GameCharacteristicValuesResponse
import org.springframework.stereotype.Service

/**
 * 个体特征取值 Service。
 */
@Service
class GameCharacteristicValuesService(
	repository: GameCharacteristicValuesRepository,
) : GameDataTableService<GameCharacteristicValuesRequest, GameCharacteristicValuesResponse>(
	repository,
	GameCharacteristicValuesResponse::from,
)
