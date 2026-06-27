package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameItemRepository
import io.github.lishangbu.gamedata.dto.GameItemRequest
import io.github.lishangbu.gamedata.dto.GameItemResponse
import org.springframework.stereotype.Service

/**
 * 道具资料 Service。
 */
@Service
class GameItemService(
	repository: GameItemRepository,
) : GameDataTableService<GameItemRequest, GameItemResponse>(
	repository,
	GameItemResponse::from,
)
