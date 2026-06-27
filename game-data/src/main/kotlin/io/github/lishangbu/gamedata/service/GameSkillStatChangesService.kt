package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSkillStatChangesRepository
import io.github.lishangbu.gamedata.dto.GameSkillStatChangesRequest
import io.github.lishangbu.gamedata.dto.GameSkillStatChangesResponse
import org.springframework.stereotype.Service

/**
 * 技能数值变化 Service。
 */
@Service
class GameSkillStatChangesService(
	repository: GameSkillStatChangesRepository,
) : GameDataTableService<GameSkillStatChangesRequest, GameSkillStatChangesResponse>(
	repository,
	GameSkillStatChangesResponse::from,
)
