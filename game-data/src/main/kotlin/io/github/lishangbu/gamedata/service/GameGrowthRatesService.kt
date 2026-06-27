package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameGrowthRatesRepository
import io.github.lishangbu.gamedata.dto.GameGrowthRatesRequest
import io.github.lishangbu.gamedata.dto.GameGrowthRatesResponse
import org.springframework.stereotype.Service

/**
 * 成长速率 Service。
 */
@Service
class GameGrowthRatesService(
	repository: GameGrowthRatesRepository,
) : GameDataTableService<GameGrowthRatesRequest, GameGrowthRatesResponse>(
	repository,
	GameGrowthRatesResponse::from,
)
