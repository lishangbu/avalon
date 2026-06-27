package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameItemAttributeBindingsRepository
import io.github.lishangbu.gamedata.dto.GameItemAttributeBindingsRequest
import io.github.lishangbu.gamedata.dto.GameItemAttributeBindingsResponse
import org.springframework.stereotype.Service

/**
 * 道具属性绑定 Service。
 */
@Service
class GameItemAttributeBindingsService(
	repository: GameItemAttributeBindingsRepository,
) : GameDataTableService<GameItemAttributeBindingsRequest, GameItemAttributeBindingsResponse>(
	repository,
	GameItemAttributeBindingsResponse::from,
)
