package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSkillAilmentsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 技能异常管理接口。
 */
@RestController
@RequestMapping("/api/game-data/skill-ailments")
@Tag(name = "游戏资料 - 技能异常")
class GameSkillAilmentsController(
	service: GameSkillAilmentsService,
) : GameDataCrudControllerSupport(service)
