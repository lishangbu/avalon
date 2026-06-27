package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSkillDetailsRepository
import io.github.lishangbu.gamedata.dto.GameSkillDetailsRequest
import io.github.lishangbu.gamedata.dto.GameSkillDetailsResponse
import org.springframework.stereotype.Service

/**
 * 技能详情 Service。
 */
@Service
class GameSkillDetailsService(
	repository: GameSkillDetailsRepository,
) : GameDataTableService<GameSkillDetailsRequest, GameSkillDetailsResponse>(
	repository,
	GameSkillDetailsResponse::from,
)
