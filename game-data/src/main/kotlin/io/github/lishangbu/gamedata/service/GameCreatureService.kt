package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCreatureRepository
import io.github.lishangbu.gamedata.dto.GameCreatureRequest
import io.github.lishangbu.gamedata.dto.GameCreatureResponse
import org.springframework.stereotype.Service

/**
 * 生物资料 Service。
 */
@Service
class GameCreatureService(
	repository: GameCreatureRepository,
) : GameDataTableService<GameCreatureRequest, GameCreatureResponse>(
	repository,
	GameCreatureResponse::from,
)
