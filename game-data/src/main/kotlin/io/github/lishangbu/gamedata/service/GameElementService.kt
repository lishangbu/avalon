package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameElementRepository
import io.github.lishangbu.gamedata.dto.GameElementRequest
import io.github.lishangbu.gamedata.dto.GameElementResponse
import org.springframework.stereotype.Service

/**
 * 属性资料 Service。
 */
@Service
class GameElementService(
	repository: GameElementRepository,
) : GameDataTableService<GameElementRequest, GameElementResponse>(
	repository,
	GameElementResponse::from,
)
