package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameItemAttributesRepository
import io.github.lishangbu.gamedata.dto.GameItemAttributesRequest
import io.github.lishangbu.gamedata.dto.GameItemAttributesResponse
import org.springframework.stereotype.Service

/**
 * 道具属性 Service。
 */
@Service
class GameItemAttributesService(
	repository: GameItemAttributesRepository,
) : GameDataTableService<GameItemAttributesRequest, GameItemAttributesResponse>(
	repository,
	GameItemAttributesResponse::from,
)
