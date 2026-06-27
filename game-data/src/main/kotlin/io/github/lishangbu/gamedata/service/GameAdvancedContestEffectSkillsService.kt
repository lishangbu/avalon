package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameAdvancedContestEffectSkillsRepository
import io.github.lishangbu.gamedata.dto.GameAdvancedContestEffectSkillsRequest
import io.github.lishangbu.gamedata.dto.GameAdvancedContestEffectSkillsResponse
import org.springframework.stereotype.Service

/**
 * 高级评价效果技能 Service。
 */
@Service
class GameAdvancedContestEffectSkillsService(
	repository: GameAdvancedContestEffectSkillsRepository,
) : GameDataTableService<GameAdvancedContestEffectSkillsRequest, GameAdvancedContestEffectSkillsResponse>(
	repository,
	GameAdvancedContestEffectSkillsResponse::from,
)
