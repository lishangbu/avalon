package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameElementService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 属性资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/elements")
@Tag(name = "游戏资料 - 属性资料")
class GameElementController(
	service: GameElementService,
) : GameDataCrudControllerSupport(service)
