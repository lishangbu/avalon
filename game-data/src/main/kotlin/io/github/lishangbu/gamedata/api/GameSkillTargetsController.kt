package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSkillTargetsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 技能目标管理接口。
 */
@RestController
@RequestMapping("/api/game-data/skill-targets")
@Tag(name = "游戏资料 - 技能目标")
class GameSkillTargetsController(
	service: GameSkillTargetsService,
) : GameDataCrudControllerSupport(service)
