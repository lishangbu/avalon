package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameNatureBattleStylePreferencesRepository
import io.github.lishangbu.gamedata.dto.GameNatureBattleStylePreferencesRequest
import io.github.lishangbu.gamedata.dto.GameNatureBattleStylePreferencesResponse
import org.springframework.stereotype.Service

/**
 * 性格战斗风格偏好 Service。
 */
@Service
class GameNatureBattleStylePreferencesService(
	repository: GameNatureBattleStylePreferencesRepository,
) : GameDataTableService<GameNatureBattleStylePreferencesRequest, GameNatureBattleStylePreferencesResponse>(
	repository,
	GameNatureBattleStylePreferencesResponse::from,
)
