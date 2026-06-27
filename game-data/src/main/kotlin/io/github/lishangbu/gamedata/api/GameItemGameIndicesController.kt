package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameItemGameIndicesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 道具版本索引管理接口。
 */
@RestController
@RequestMapping("/api/game-data/item-game-indices")
@Tag(name = "游戏资料 - 道具版本索引")
class GameItemGameIndicesController(
	service: GameItemGameIndicesService,
) : GameDataCrudControllerSupport(service)
