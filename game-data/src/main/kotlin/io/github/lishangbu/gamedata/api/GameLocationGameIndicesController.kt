package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameLocationGameIndicesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 地点版本索引管理接口。
 */
@RestController
@RequestMapping("/api/game-data/location-game-indices")
@Tag(name = "游戏资料 - 地点版本索引")
class GameLocationGameIndicesController(
	service: GameLocationGameIndicesService,
) : GameDataCrudControllerSupport(service)
