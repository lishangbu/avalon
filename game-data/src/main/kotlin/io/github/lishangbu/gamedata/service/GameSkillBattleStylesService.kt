package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSkillBattleStylesRepository
import io.github.lishangbu.gamedata.dto.GameSkillBattleStylesRequest
import io.github.lishangbu.gamedata.dto.GameSkillBattleStylesResponse
import org.springframework.stereotype.Service

/**
 * 技能战斗风格 Service。
 */
@Service
class GameSkillBattleStylesService(
	repository: GameSkillBattleStylesRepository,
) : GameDataTableService<GameSkillBattleStylesRequest, GameSkillBattleStylesResponse>(
	repository,
	GameSkillBattleStylesResponse::from,
)
