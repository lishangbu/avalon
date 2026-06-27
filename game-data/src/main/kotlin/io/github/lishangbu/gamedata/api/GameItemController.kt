package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameItemService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 道具资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/items")
@Tag(name = "游戏资料 - 道具资料")
class GameItemController(
	service: GameItemService,
) : GameDataCrudControllerSupport(service)
