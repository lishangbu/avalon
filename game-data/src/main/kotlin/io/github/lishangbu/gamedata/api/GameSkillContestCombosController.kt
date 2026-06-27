package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSkillContestCombosService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 技能评价组合管理接口。
 */
@RestController
@RequestMapping("/api/game-data/skill-contest-combos")
@Tag(name = "游戏资料 - 技能评价组合")
class GameSkillContestCombosController(
	service: GameSkillContestCombosService,
) : GameDataCrudControllerSupport(service)
