package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameItemCategoryRepository
import io.github.lishangbu.gamedata.dto.GameItemCategoryRequest
import io.github.lishangbu.gamedata.dto.GameItemCategoryResponse
import org.springframework.stereotype.Service

/**
 * 道具分类 Service。
 */
@Service
class GameItemCategoryService(
	repository: GameItemCategoryRepository,
) : GameDataTableService<GameItemCategoryRequest, GameItemCategoryResponse>(
	repository,
	GameItemCategoryResponse::from,
)
