package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameAbilityRepository
import io.github.lishangbu.gamedata.dto.GameAbilityRequest
import io.github.lishangbu.gamedata.dto.GameAbilityResponse
import org.springframework.stereotype.Service

/**
 * 特性资料 Service。
 */
@Service
class GameAbilityService(
	repository: GameAbilityRepository,
) : GameDataTableService<GameAbilityRequest, GameAbilityResponse>(
	repository,
	GameAbilityResponse::from,
)
