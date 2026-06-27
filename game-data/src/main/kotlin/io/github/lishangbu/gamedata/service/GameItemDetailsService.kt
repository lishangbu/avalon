package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameItemDetailsRepository
import io.github.lishangbu.gamedata.dto.GameItemDetailsRequest
import io.github.lishangbu.gamedata.dto.GameItemDetailsResponse
import org.springframework.stereotype.Service

/**
 * 道具详情 Service。
 */
@Service
class GameItemDetailsService(
	repository: GameItemDetailsRepository,
) : GameDataTableService<GameItemDetailsRequest, GameItemDetailsResponse>(
	repository,
	GameItemDetailsResponse::from,
)
