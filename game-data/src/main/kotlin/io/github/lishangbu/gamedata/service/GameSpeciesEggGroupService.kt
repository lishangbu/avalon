package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSpeciesEggGroupRepository
import io.github.lishangbu.gamedata.dto.GameSpeciesEggGroupRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesEggGroupResponse
import org.springframework.stereotype.Service

/**
 * 种类分组绑定 Service。
 */
@Service
class GameSpeciesEggGroupService(
	repository: GameSpeciesEggGroupRepository,
) : GameDataTableService<GameSpeciesEggGroupRequest, GameSpeciesEggGroupResponse>(
	repository,
	GameSpeciesEggGroupResponse::from,
)
