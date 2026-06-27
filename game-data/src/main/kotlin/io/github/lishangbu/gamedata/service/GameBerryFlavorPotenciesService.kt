package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameBerryFlavorPotenciesRepository
import io.github.lishangbu.gamedata.dto.GameBerryFlavorPotenciesRequest
import io.github.lishangbu.gamedata.dto.GameBerryFlavorPotenciesResponse
import org.springframework.stereotype.Service

/**
 * 树果口味强度 Service。
 */
@Service
class GameBerryFlavorPotenciesService(
	repository: GameBerryFlavorPotenciesRepository,
) : GameDataTableService<GameBerryFlavorPotenciesRequest, GameBerryFlavorPotenciesResponse>(
	repository,
	GameBerryFlavorPotenciesResponse::from,
)
