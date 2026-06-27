package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameBerryFlavorsRepository
import io.github.lishangbu.gamedata.dto.GameBerryFlavorsRequest
import io.github.lishangbu.gamedata.dto.GameBerryFlavorsResponse
import org.springframework.stereotype.Service

/**
 * 树果口味 Service。
 */
@Service
class GameBerryFlavorsService(
	repository: GameBerryFlavorsRepository,
) : GameDataTableService<GameBerryFlavorsRequest, GameBerryFlavorsResponse>(
	repository,
	GameBerryFlavorsResponse::from,
)
