package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSpeciesCatalogNumbersRepository
import io.github.lishangbu.gamedata.dto.GameSpeciesCatalogNumbersRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesCatalogNumbersResponse
import org.springframework.stereotype.Service

/**
 * 种类目录编号 Service。
 */
@Service
class GameSpeciesCatalogNumbersService(
	repository: GameSpeciesCatalogNumbersRepository,
) : GameDataTableService<GameSpeciesCatalogNumbersRequest, GameSpeciesCatalogNumbersResponse>(
	repository,
	GameSpeciesCatalogNumbersResponse::from,
)
