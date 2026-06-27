package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCreatureElementRepository
import io.github.lishangbu.gamedata.dto.GameCreatureElementRequest
import io.github.lishangbu.gamedata.dto.GameCreatureElementResponse
import org.springframework.stereotype.Service

/**
 * 生物属性绑定 Service。
 */
@Service
class GameCreatureElementService(
	repository: GameCreatureElementRepository,
) : GameDataTableService<GameCreatureElementRequest, GameCreatureElementResponse>(
	repository,
	GameCreatureElementResponse::from,
)
