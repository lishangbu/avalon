package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCreatureAbilityRepository
import io.github.lishangbu.gamedata.dto.GameCreatureAbilityRequest
import io.github.lishangbu.gamedata.dto.GameCreatureAbilityResponse
import org.springframework.stereotype.Service

/**
 * 生物特性绑定 Service。
 */
@Service
class GameCreatureAbilityService(
	repository: GameCreatureAbilityRepository,
) : GameDataTableService<GameCreatureAbilityRequest, GameCreatureAbilityResponse>(
	repository,
	GameCreatureAbilityResponse::from,
)
