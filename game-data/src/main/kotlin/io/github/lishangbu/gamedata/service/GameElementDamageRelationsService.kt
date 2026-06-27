package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameElementDamageRelationsRepository
import io.github.lishangbu.gamedata.dto.GameElementDamageRelationsRequest
import io.github.lishangbu.gamedata.dto.GameElementDamageRelationsResponse
import org.springframework.stereotype.Service

/**
 * 属性克制关系 Service。
 */
@Service
class GameElementDamageRelationsService(
	repository: GameElementDamageRelationsRepository,
) : GameDataTableService<GameElementDamageRelationsRequest, GameElementDamageRelationsResponse>(
	repository,
	GameElementDamageRelationsResponse::from,
)
