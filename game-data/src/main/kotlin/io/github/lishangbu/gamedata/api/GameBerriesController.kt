package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameBerriesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 树果资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/berries")
@Tag(name = "游戏资料 - 树果资料")
class GameBerriesController(
	service: GameBerriesService,
) : GameDataCrudControllerSupport(service)
