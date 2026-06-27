package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSkillService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 技能资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/skills")
@Tag(name = "游戏资料 - 技能资料")
class GameSkillController(
	service: GameSkillService,
) : GameDataCrudControllerSupport(service)
