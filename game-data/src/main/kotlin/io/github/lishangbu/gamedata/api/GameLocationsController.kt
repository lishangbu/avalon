package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameLocationsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 地点资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/locations")
@Tag(name = "游戏资料 - 地点资料")
class GameLocationsController(
	service: GameLocationsService,
) : GameDataCrudControllerSupport(service)
