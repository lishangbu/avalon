package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameItemCategoryPocketsRepository
import io.github.lishangbu.gamedata.dto.GameItemCategoryPocketsRequest
import io.github.lishangbu.gamedata.dto.GameItemCategoryPocketsResponse
import org.springframework.stereotype.Service

/**
 * 道具分类口袋 Service。
 */
@Service
class GameItemCategoryPocketsService(
	repository: GameItemCategoryPocketsRepository,
) : GameDataTableService<GameItemCategoryPocketsRequest, GameItemCategoryPocketsResponse>(
	repository,
	GameItemCategoryPocketsResponse::from,
)
