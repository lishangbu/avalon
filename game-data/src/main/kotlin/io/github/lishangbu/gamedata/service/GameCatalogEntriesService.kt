package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCatalogEntriesRepository
import io.github.lishangbu.gamedata.dto.GameCatalogEntriesRequest
import io.github.lishangbu.gamedata.dto.GameCatalogEntriesResponse
import org.springframework.stereotype.Service

/**
 * 图鉴目录条目 Service。
 */
@Service
class GameCatalogEntriesService(
	repository: GameCatalogEntriesRepository,
) : GameDataTableService<GameCatalogEntriesRequest, GameCatalogEntriesResponse>(
	repository,
	GameCatalogEntriesResponse::from,
)
