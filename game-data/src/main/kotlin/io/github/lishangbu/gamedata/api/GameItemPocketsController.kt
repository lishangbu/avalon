package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameItemPocketsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 道具口袋管理接口。
 */
@RestController
@RequestMapping("/api/game-data/item-pockets")
@Tag(name = "游戏资料 - 道具口袋")
class GameItemPocketsController(
	service: GameItemPocketsService,
) : GameDataCrudControllerSupport(service)
