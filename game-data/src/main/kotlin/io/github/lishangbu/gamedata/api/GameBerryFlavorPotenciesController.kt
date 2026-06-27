package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameBerryFlavorPotenciesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 树果口味强度管理接口。
 */
@RestController
@RequestMapping("/api/game-data/berry-flavor-potencies")
@Tag(name = "游戏资料 - 树果口味强度")
class GameBerryFlavorPotenciesController(
	service: GameBerryFlavorPotenciesService,
) : GameDataCrudControllerSupport(service)
