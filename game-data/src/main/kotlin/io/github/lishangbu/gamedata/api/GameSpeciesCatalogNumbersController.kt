package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSpeciesCatalogNumbersService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 种类目录编号管理接口。
 */
@RestController
@RequestMapping("/api/game-data/species-catalog-numbers")
@Tag(name = "游戏资料 - 种类目录编号")
class GameSpeciesCatalogNumbersController(
	service: GameSpeciesCatalogNumbersService,
) : GameDataCrudControllerSupport(service)
