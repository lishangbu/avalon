package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSkillStatChangesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 技能数值变化管理接口。
 */
@RestController
@RequestMapping("/api/game-data/skill-stat-changes")
@Tag(name = "游戏资料 - 技能数值变化")
class GameSkillStatChangesController(
	service: GameSkillStatChangesService,
) : GameDataCrudControllerSupport(service)
