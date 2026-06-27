package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameElementGameIndicesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 属性版本索引管理接口。
 */
@RestController
@RequestMapping("/api/game-data/element-game-indices")
@Tag(name = "游戏资料 - 属性版本索引")
class GameElementGameIndicesController(
	service: GameElementGameIndicesService,
) : GameDataCrudControllerSupport(service)
