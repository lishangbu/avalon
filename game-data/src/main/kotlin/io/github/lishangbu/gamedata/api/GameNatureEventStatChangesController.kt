package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameNatureEventStatChangesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 性格活动能力变化管理接口。
 */
@RestController
@RequestMapping("/api/game-data/nature-event-stat-changes")
@Tag(name = "游戏资料 - 性格活动能力变化")
class GameNatureEventStatChangesController(
	service: GameNatureEventStatChangesService,
) : GameDataCrudControllerSupport(service)
