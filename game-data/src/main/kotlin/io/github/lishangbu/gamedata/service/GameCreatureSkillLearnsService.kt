package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameCreatureSkillLearnsRepository
import io.github.lishangbu.gamedata.dto.GameCreatureSkillLearnsRequest
import io.github.lishangbu.gamedata.dto.GameCreatureSkillLearnsResponse
import org.springframework.stereotype.Service

/**
 * 生物技能学习 Service。
 */
@Service
class GameCreatureSkillLearnsService(
	repository: GameCreatureSkillLearnsRepository,
) : GameDataTableService<GameCreatureSkillLearnsRequest, GameCreatureSkillLearnsResponse>(
	repository,
	GameCreatureSkillLearnsResponse::from,
)
