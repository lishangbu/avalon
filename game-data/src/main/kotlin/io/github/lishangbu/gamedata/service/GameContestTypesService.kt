package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameContestTypesRepository
import io.github.lishangbu.gamedata.dto.GameContestTypesRequest
import io.github.lishangbu.gamedata.dto.GameContestTypesResponse
import org.springframework.stereotype.Service

/**
 * 评分类别 Service。
 */
@Service
class GameContestTypesService(
	repository: GameContestTypesRepository,
) : GameDataTableService<GameContestTypesRequest, GameContestTypesResponse>(
	repository,
	GameContestTypesResponse::from,
)
