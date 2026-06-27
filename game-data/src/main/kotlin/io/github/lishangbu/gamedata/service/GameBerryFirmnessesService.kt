package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameBerryFirmnessesRepository
import io.github.lishangbu.gamedata.dto.GameBerryFirmnessesRequest
import io.github.lishangbu.gamedata.dto.GameBerryFirmnessesResponse
import org.springframework.stereotype.Service

/**
 * 树果硬度 Service。
 */
@Service
class GameBerryFirmnessesService(
	repository: GameBerryFirmnessesRepository,
) : GameDataTableService<GameBerryFirmnessesRequest, GameBerryFirmnessesResponse>(
	repository,
	GameBerryFirmnessesResponse::from,
)
