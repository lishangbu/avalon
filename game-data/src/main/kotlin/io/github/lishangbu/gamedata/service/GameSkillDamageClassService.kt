package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSkillDamageClassRepository
import io.github.lishangbu.gamedata.dto.GameSkillDamageClassRequest
import io.github.lishangbu.gamedata.dto.GameSkillDamageClassResponse
import org.springframework.stereotype.Service

/**
 * 技能分类 Service。
 */
@Service
class GameSkillDamageClassService(
	repository: GameSkillDamageClassRepository,
) : GameDataTableService<GameSkillDamageClassRequest, GameSkillDamageClassResponse>(
	repository,
	GameSkillDamageClassResponse::from,
)
