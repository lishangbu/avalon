package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSpeciesDetailsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 种类详情管理接口。
 */
@RestController
@RequestMapping("/api/game-data/species-details")
@Tag(name = "游戏资料 - 种类详情")
class GameSpeciesDetailsController(
	service: GameSpeciesDetailsService,
) : GameDataCrudControllerSupport(service)
