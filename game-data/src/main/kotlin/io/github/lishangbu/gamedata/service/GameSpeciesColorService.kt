package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSpeciesColorRepository
import io.github.lishangbu.gamedata.dto.GameSpeciesColorRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesColorResponse
import org.springframework.stereotype.Service

/**
 * 种类颜色 Service。
 */
@Service
class GameSpeciesColorService(
	repository: GameSpeciesColorRepository,
) : GameDataTableService<GameSpeciesColorRequest, GameSpeciesColorResponse>(
	repository,
	GameSpeciesColorResponse::from,
)
