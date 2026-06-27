package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSkillAilmentsRepository
import io.github.lishangbu.gamedata.dto.GameSkillAilmentsRequest
import io.github.lishangbu.gamedata.dto.GameSkillAilmentsResponse
import org.springframework.stereotype.Service

/**
 * 技能异常 Service。
 */
@Service
class GameSkillAilmentsService(
	repository: GameSkillAilmentsRepository,
) : GameDataTableService<GameSkillAilmentsRequest, GameSkillAilmentsResponse>(
	repository,
	GameSkillAilmentsResponse::from,
)
