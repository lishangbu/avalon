package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameNatureEventStatChangesRepository
import io.github.lishangbu.gamedata.dto.GameNatureEventStatChangesRequest
import io.github.lishangbu.gamedata.dto.GameNatureEventStatChangesResponse
import org.springframework.stereotype.Service

/**
 * 性格活动能力变化 Service。
 */
@Service
class GameNatureEventStatChangesService(
	repository: GameNatureEventStatChangesRepository,
) : GameDataTableService<GameNatureEventStatChangesRequest, GameNatureEventStatChangesResponse>(
	repository,
	GameNatureEventStatChangesResponse::from,
)
