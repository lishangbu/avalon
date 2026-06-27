package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameEventStatsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 活动能力项管理接口。
 */
@RestController
@RequestMapping("/api/game-data/event-stats")
@Tag(name = "游戏资料 - 活动能力项")
class GameEventStatsController(
	service: GameEventStatsService,
) : GameDataCrudControllerSupport(service)
