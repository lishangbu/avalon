package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSpeciesShapeService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 种类形态管理接口。
 */
@RestController
@RequestMapping("/api/game-data/species-shapes")
@Tag(name = "游戏资料 - 种类形态")
class GameSpeciesShapeController(
	service: GameSpeciesShapeService,
) : GameDataCrudControllerSupport(service)
