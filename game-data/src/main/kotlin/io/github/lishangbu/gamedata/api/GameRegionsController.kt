package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameRegionsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 地区资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/regions")
@Tag(name = "游戏资料 - 地区资料")
class GameRegionsController(
	service: GameRegionsService,
) : GameDataCrudControllerSupport(service)
