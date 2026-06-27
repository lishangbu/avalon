package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameGrowthRateLevelsRepository
import io.github.lishangbu.gamedata.dto.GameGrowthRateLevelsRequest
import io.github.lishangbu.gamedata.dto.GameGrowthRateLevelsResponse
import org.springframework.stereotype.Service

/**
 * 成长等级经验 Service。
 */
@Service
class GameGrowthRateLevelsService(
	repository: GameGrowthRateLevelsRepository,
) : GameDataTableService<GameGrowthRateLevelsRequest, GameGrowthRateLevelsResponse>(
	repository,
	GameGrowthRateLevelsResponse::from,
)
