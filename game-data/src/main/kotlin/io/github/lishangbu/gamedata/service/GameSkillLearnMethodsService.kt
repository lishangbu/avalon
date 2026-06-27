package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSkillLearnMethodsRepository
import io.github.lishangbu.gamedata.dto.GameSkillLearnMethodsRequest
import io.github.lishangbu.gamedata.dto.GameSkillLearnMethodsResponse
import org.springframework.stereotype.Service

/**
 * 技能学习方式 Service。
 */
@Service
class GameSkillLearnMethodsService(
	repository: GameSkillLearnMethodsRepository,
) : GameDataTableService<GameSkillLearnMethodsRequest, GameSkillLearnMethodsResponse>(
	repository,
	GameSkillLearnMethodsResponse::from,
)
