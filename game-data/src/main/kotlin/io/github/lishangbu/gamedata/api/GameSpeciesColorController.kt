package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSpeciesColorService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 种类颜色管理接口。
 */
@RestController
@RequestMapping("/api/game-data/species-colors")
@Tag(name = "游戏资料 - 种类颜色")
class GameSpeciesColorController(
	service: GameSpeciesColorService,
) : GameDataCrudControllerSupport(service)
