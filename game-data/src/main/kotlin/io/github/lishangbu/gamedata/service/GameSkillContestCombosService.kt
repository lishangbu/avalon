package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameSkillContestCombosRepository
import io.github.lishangbu.gamedata.dto.GameSkillContestCombosRequest
import io.github.lishangbu.gamedata.dto.GameSkillContestCombosResponse
import org.springframework.stereotype.Service

/**
 * 技能评价组合 Service。
 */
@Service
class GameSkillContestCombosService(
	repository: GameSkillContestCombosRepository,
) : GameDataTableService<GameSkillContestCombosRequest, GameSkillContestCombosResponse>(
	repository,
	GameSkillContestCombosResponse::from,
)
