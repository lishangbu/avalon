package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCatalogsRepository
import io.github.lishangbu.gamedata.dto.GameCatalogsRequest
import io.github.lishangbu.gamedata.dto.GameCatalogsResponse
import org.springframework.stereotype.Service

/**
 * 图鉴目录 Service。
 */
@Service
class GameCatalogsService(
	repository: GameCatalogsRepository,
) : GameDataTableService<GameCatalogsRequest, GameCatalogsResponse>(
	repository,
	GameCatalogsResponse::from,
)
