package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameLocationAreaMethodRatesRepository
import io.github.lishangbu.gamedata.dto.GameLocationAreaMethodRatesRequest
import io.github.lishangbu.gamedata.dto.GameLocationAreaMethodRatesResponse
import org.springframework.stereotype.Service

/**
 * 区域遭遇方式概率 Service。
 */
@Service
class GameLocationAreaMethodRatesService(
	repository: GameLocationAreaMethodRatesRepository,
) : GameDataTableService<GameLocationAreaMethodRatesRequest, GameLocationAreaMethodRatesResponse>(
	repository,
	GameLocationAreaMethodRatesResponse::from,
)
