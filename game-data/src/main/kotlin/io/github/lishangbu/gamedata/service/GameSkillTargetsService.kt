package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSkillTargetsRepository
import io.github.lishangbu.gamedata.dto.GameSkillTargetsRequest
import io.github.lishangbu.gamedata.dto.GameSkillTargetsResponse
import org.springframework.stereotype.Service

/**
 * 技能目标 Service。
 */
@Service
class GameSkillTargetsService(
	repository: GameSkillTargetsRepository,
) : GameDataTableService<GameSkillTargetsRequest, GameSkillTargetsResponse>(
	repository,
	GameSkillTargetsResponse::from,
)
