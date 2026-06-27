package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameEncounterConditionsRepository
import io.github.lishangbu.gamedata.dto.GameEncounterConditionsRequest
import io.github.lishangbu.gamedata.dto.GameEncounterConditionsResponse
import org.springframework.stereotype.Service

/**
 * 遭遇条件 Service。
 */
@Service
class GameEncounterConditionsService(
	repository: GameEncounterConditionsRepository,
) : GameDataTableService<GameEncounterConditionsRequest, GameEncounterConditionsResponse>(
	repository,
	GameEncounterConditionsResponse::from,
)
