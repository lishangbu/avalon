package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameBerryFlavorsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 树果口味管理接口。
 */
@RestController
@RequestMapping("/api/game-data/berry-flavors")
@Tag(name = "游戏资料 - 树果口味")
class GameBerryFlavorsController(
	service: GameBerryFlavorsService,
) : GameDataCrudControllerSupport(service)
