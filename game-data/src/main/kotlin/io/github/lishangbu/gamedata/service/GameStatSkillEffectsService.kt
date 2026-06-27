package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameStatSkillEffectsRepository
import io.github.lishangbu.gamedata.dto.GameStatSkillEffectsRequest
import io.github.lishangbu.gamedata.dto.GameStatSkillEffectsResponse
import org.springframework.stereotype.Service

/**
 * 数值项技能影响 Service。
 */
@Service
class GameStatSkillEffectsService(
	repository: GameStatSkillEffectsRepository,
) : GameDataTableService<GameStatSkillEffectsRequest, GameStatSkillEffectsResponse>(
	repository,
	GameStatSkillEffectsResponse::from,
)
