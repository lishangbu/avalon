package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameGendersRepository
import io.github.lishangbu.gamedata.dto.GameGendersRequest
import io.github.lishangbu.gamedata.dto.GameGendersResponse
import org.springframework.stereotype.Service

/**
 * 性别资料 Service。
 */
@Service
class GameGendersService(
	repository: GameGendersRepository,
) : GameDataTableService<GameGendersRequest, GameGendersResponse>(
	repository,
	GameGendersResponse::from,
)
