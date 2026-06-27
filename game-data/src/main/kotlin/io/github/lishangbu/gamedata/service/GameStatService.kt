package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameStatRepository
import io.github.lishangbu.gamedata.dto.GameStatRequest
import io.github.lishangbu.gamedata.dto.GameStatResponse
import org.springframework.stereotype.Service

/**
 * 数值项 Service。
 */
@Service
class GameStatService(
	repository: GameStatRepository,
) : GameDataTableService<GameStatRequest, GameStatResponse>(
	repository,
	GameStatResponse::from,
)
