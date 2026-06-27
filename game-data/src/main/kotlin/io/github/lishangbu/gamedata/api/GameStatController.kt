package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameStatService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 数值项管理接口。
 */
@RestController
@RequestMapping("/api/game-data/stats")
@Tag(name = "游戏资料 - 数值项")
class GameStatController(
	service: GameStatService,
) : GameDataCrudControllerSupport(service)
