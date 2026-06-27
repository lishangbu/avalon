package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSkillCategoriesRepository
import io.github.lishangbu.gamedata.dto.GameSkillCategoriesRequest
import io.github.lishangbu.gamedata.dto.GameSkillCategoriesResponse
import org.springframework.stereotype.Service

/**
 * 技能元分类 Service。
 */
@Service
class GameSkillCategoriesService(
	repository: GameSkillCategoriesRepository,
) : GameDataTableService<GameSkillCategoriesRequest, GameSkillCategoriesResponse>(
	repository,
	GameSkillCategoriesResponse::from,
)
