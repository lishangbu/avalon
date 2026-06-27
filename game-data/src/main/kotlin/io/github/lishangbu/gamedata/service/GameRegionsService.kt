package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameRegionsRepository
import io.github.lishangbu.gamedata.dto.GameRegionsRequest
import io.github.lishangbu.gamedata.dto.GameRegionsResponse
import org.springframework.stereotype.Service

/**
 * 地区资料 Service。
 */
@Service
class GameRegionsService(
	repository: GameRegionsRepository,
) : GameDataTableService<GameRegionsRequest, GameRegionsResponse>(
	repository,
	GameRegionsResponse::from,
)
