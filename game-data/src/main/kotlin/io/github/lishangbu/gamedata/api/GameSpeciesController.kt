package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSpeciesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 种类资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/species")
@Tag(name = "游戏资料 - 种类资料")
class GameSpeciesController(
	service: GameSpeciesService,
) : GameDataCrudControllerSupport(service)
