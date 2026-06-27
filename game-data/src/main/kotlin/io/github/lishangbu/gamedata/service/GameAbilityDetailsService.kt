package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameAbilityDetailsRepository
import io.github.lishangbu.gamedata.dto.GameAbilityDetailsRequest
import io.github.lishangbu.gamedata.dto.GameAbilityDetailsResponse
import org.springframework.stereotype.Service

/**
 * 特性详情 Service。
 */
@Service
class GameAbilityDetailsService(
	repository: GameAbilityDetailsRepository,
) : GameDataTableService<GameAbilityDetailsRequest, GameAbilityDetailsResponse>(
	repository,
	GameAbilityDetailsResponse::from,
)
