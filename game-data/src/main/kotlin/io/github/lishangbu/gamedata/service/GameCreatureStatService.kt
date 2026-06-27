package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCreatureStatRepository
import io.github.lishangbu.gamedata.dto.GameCreatureStatRequest
import io.github.lishangbu.gamedata.dto.GameCreatureStatResponse
import org.springframework.stereotype.Service

/**
 * 生物数值绑定 Service。
 */
@Service
class GameCreatureStatService(
	repository: GameCreatureStatRepository,
) : GameDataTableService<GameCreatureStatRequest, GameCreatureStatResponse>(
	repository,
	GameCreatureStatResponse::from,
)
