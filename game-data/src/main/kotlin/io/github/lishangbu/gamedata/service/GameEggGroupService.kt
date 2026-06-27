package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameEggGroupRepository
import io.github.lishangbu.gamedata.dto.GameEggGroupRequest
import io.github.lishangbu.gamedata.dto.GameEggGroupResponse
import org.springframework.stereotype.Service

/**
 * 种类分组 Service。
 */
@Service
class GameEggGroupService(
	repository: GameEggGroupRepository,
) : GameDataTableService<GameEggGroupRequest, GameEggGroupResponse>(
	repository,
	GameEggGroupResponse::from,
)
