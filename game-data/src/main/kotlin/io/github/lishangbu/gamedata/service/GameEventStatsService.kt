package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameEventStatsRepository
import io.github.lishangbu.gamedata.dto.GameEventStatsRequest
import io.github.lishangbu.gamedata.dto.GameEventStatsResponse
import org.springframework.stereotype.Service

/**
 * 活动能力项 Service。
 */
@Service
class GameEventStatsService(
	repository: GameEventStatsRepository,
) : GameDataTableService<GameEventStatsRequest, GameEventStatsResponse>(
	repository,
	GameEventStatsResponse::from,
)
