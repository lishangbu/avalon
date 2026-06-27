package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameEncounterConditionValuesRepository
import io.github.lishangbu.gamedata.dto.GameEncounterConditionValuesRequest
import io.github.lishangbu.gamedata.dto.GameEncounterConditionValuesResponse
import org.springframework.stereotype.Service

/**
 * 遭遇条件值 Service。
 */
@Service
class GameEncounterConditionValuesService(
	repository: GameEncounterConditionValuesRepository,
) : GameDataTableService<GameEncounterConditionValuesRequest, GameEncounterConditionValuesResponse>(
	repository,
	GameEncounterConditionValuesResponse::from,
)
