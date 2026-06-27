package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameBerryFirmnessesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 树果硬度管理接口。
 */
@RestController
@RequestMapping("/api/game-data/berry-firmnesses")
@Tag(name = "游戏资料 - 树果硬度")
class GameBerryFirmnessesController(
	service: GameBerryFirmnessesService,
) : GameDataCrudControllerSupport(service)
