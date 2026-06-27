package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameLocationAreaEncounterConditionValuesRepository
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncounterConditionValuesRequest
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncounterConditionValuesResponse
import org.springframework.stereotype.Service

/**
 * 区域遭遇条件绑定 Service。
 */
@Service
class GameLocationAreaEncounterConditionValuesService(
	repository: GameLocationAreaEncounterConditionValuesRepository,
) : GameDataTableService<GameLocationAreaEncounterConditionValuesRequest, GameLocationAreaEncounterConditionValuesResponse>(
	repository,
	GameLocationAreaEncounterConditionValuesResponse::from,
)
