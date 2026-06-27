package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameLocationAreaEncountersRepository
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncountersRequest
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncountersResponse
import org.springframework.stereotype.Service

/**
 * 区域生物遭遇 Service。
 */
@Service
class GameLocationAreaEncountersService(
	repository: GameLocationAreaEncountersRepository,
) : GameDataTableService<GameLocationAreaEncountersRequest, GameLocationAreaEncountersResponse>(
	repository,
	GameLocationAreaEncountersResponse::from,
)
