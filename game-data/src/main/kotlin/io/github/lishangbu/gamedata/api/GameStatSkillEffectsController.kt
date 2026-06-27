package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameStatSkillEffectsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 数值项技能影响管理接口。
 */
@RestController
@RequestMapping("/api/game-data/stat-skill-effects")
@Tag(name = "游戏资料 - 数值项技能影响")
class GameStatSkillEffectsController(
	service: GameStatSkillEffectsService,
) : GameDataCrudControllerSupport(service)
