package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameEvolutionNodesRepository
import io.github.lishangbu.gamedata.dto.GameEvolutionNodesRequest
import io.github.lishangbu.gamedata.dto.GameEvolutionNodesResponse
import org.springframework.stereotype.Service

/**
 * 进化链节点 Service。
 */
@Service
class GameEvolutionNodesService(
	repository: GameEvolutionNodesRepository,
) : GameDataTableService<GameEvolutionNodesRequest, GameEvolutionNodesResponse>(
	repository,
	GameEvolutionNodesResponse::from,
)
