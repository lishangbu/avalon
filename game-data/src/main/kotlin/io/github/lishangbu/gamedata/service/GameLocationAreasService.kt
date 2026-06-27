package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameLocationAreasRepository
import io.github.lishangbu.gamedata.dto.GameLocationAreasRequest
import io.github.lishangbu.gamedata.dto.GameLocationAreasResponse
import org.springframework.stereotype.Service

/**
 * 地点区域 Service。
 */
@Service
class GameLocationAreasService(
	repository: GameLocationAreasRepository,
) : GameDataTableService<GameLocationAreasRequest, GameLocationAreasResponse>(
	repository,
	GameLocationAreasResponse::from,
)
