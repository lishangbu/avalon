package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameTransferAreaSpeciesRepository
import io.github.lishangbu.gamedata.dto.GameTransferAreaSpeciesRequest
import io.github.lishangbu.gamedata.dto.GameTransferAreaSpeciesResponse
import org.springframework.stereotype.Service

/**
 * 迁移区域种类 Service。
 */
@Service
class GameTransferAreaSpeciesService(
	repository: GameTransferAreaSpeciesRepository,
) : GameDataTableService<GameTransferAreaSpeciesRequest, GameTransferAreaSpeciesResponse>(
	repository,
	GameTransferAreaSpeciesResponse::from,
)
