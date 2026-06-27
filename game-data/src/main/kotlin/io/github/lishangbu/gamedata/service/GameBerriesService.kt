package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameBerriesRepository
import io.github.lishangbu.gamedata.dto.GameBerriesRequest
import io.github.lishangbu.gamedata.dto.GameBerriesResponse
import org.springframework.stereotype.Service

/**
 * 树果资料 Service。
 */
@Service
class GameBerriesService(
	repository: GameBerriesRepository,
) : GameDataTableService<GameBerriesRequest, GameBerriesResponse>(
	repository,
	GameBerriesResponse::from,
)
