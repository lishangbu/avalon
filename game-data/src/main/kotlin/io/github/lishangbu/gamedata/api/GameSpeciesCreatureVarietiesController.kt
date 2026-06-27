package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSpeciesCreatureVarietiesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 种类生物变种管理接口。
 */
@RestController
@RequestMapping("/api/game-data/species-creature-varieties")
@Tag(name = "游戏资料 - 种类生物变种")
class GameSpeciesCreatureVarietiesController(
	service: GameSpeciesCreatureVarietiesService,
) : GameDataCrudControllerSupport(service)
