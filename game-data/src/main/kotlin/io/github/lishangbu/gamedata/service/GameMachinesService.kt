package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameMachinesRepository
import io.github.lishangbu.gamedata.dto.GameMachinesRequest
import io.github.lishangbu.gamedata.dto.GameMachinesResponse
import org.springframework.stereotype.Service

/**
 * 机器资料 Service。
 */
@Service
class GameMachinesService(
	repository: GameMachinesRepository,
) : GameDataTableService<GameMachinesRequest, GameMachinesResponse>(
	repository,
	GameMachinesResponse::from,
)
