package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCreatureFormsRepository
import io.github.lishangbu.gamedata.dto.GameCreatureFormsRequest
import io.github.lishangbu.gamedata.dto.GameCreatureFormsResponse
import org.springframework.stereotype.Service

/**
 * 生物形态 Service。
 */
@Service
class GameCreatureFormsService(
	repository: GameCreatureFormsRepository,
) : GameDataTableService<GameCreatureFormsRequest, GameCreatureFormsResponse>(
	repository,
	GameCreatureFormsResponse::from,
)
