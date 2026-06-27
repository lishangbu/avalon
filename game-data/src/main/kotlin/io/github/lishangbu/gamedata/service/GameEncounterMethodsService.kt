package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameEncounterMethodsRepository
import io.github.lishangbu.gamedata.dto.GameEncounterMethodsRequest
import io.github.lishangbu.gamedata.dto.GameEncounterMethodsResponse
import org.springframework.stereotype.Service

/**
 * 遭遇方式 Service。
 */
@Service
class GameEncounterMethodsService(
	repository: GameEncounterMethodsRepository,
) : GameDataTableService<GameEncounterMethodsRequest, GameEncounterMethodsResponse>(
	repository,
	GameEncounterMethodsResponse::from,
)
