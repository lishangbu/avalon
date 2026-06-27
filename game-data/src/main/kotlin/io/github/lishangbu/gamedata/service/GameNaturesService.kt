package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameNaturesRepository
import io.github.lishangbu.gamedata.dto.GameNaturesRequest
import io.github.lishangbu.gamedata.dto.GameNaturesResponse
import org.springframework.stereotype.Service

/**
 * 性格资料 Service。
 */
@Service
class GameNaturesService(
	repository: GameNaturesRepository,
) : GameDataTableService<GameNaturesRequest, GameNaturesResponse>(
	repository,
	GameNaturesResponse::from,
)
