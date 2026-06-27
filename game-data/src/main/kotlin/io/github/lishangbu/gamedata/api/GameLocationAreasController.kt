package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameLocationAreasService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 地点区域管理接口。
 */
@RestController
@RequestMapping("/api/game-data/location-areas")
@Tag(name = "游戏资料 - 地点区域")
class GameLocationAreasController(
	service: GameLocationAreasService,
) : GameDataCrudControllerSupport(service)
