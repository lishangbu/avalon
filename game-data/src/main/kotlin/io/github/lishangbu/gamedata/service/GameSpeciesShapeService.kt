package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSpeciesShapeRepository
import io.github.lishangbu.gamedata.dto.GameSpeciesShapeRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesShapeResponse
import org.springframework.stereotype.Service

/**
 * 种类形态 Service。
 */
@Service
class GameSpeciesShapeService(
	repository: GameSpeciesShapeRepository,
) : GameDataTableService<GameSpeciesShapeRequest, GameSpeciesShapeResponse>(
	repository,
	GameSpeciesShapeResponse::from,
)
