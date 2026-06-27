package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSkillRepository
import io.github.lishangbu.gamedata.dto.GameSkillRequest
import io.github.lishangbu.gamedata.dto.GameSkillResponse
import org.springframework.stereotype.Service

/**
 * 技能资料 Service。
 */
@Service
class GameSkillService(
	repository: GameSkillRepository,
) : GameDataTableService<GameSkillRequest, GameSkillResponse>(
	repository,
	GameSkillResponse::from,
)
