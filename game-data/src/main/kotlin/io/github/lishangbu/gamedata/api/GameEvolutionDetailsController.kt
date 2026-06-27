package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameEvolutionDetailsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 进化条件管理接口。
 */
@RestController
@RequestMapping("/api/game-data/evolution-details")
@Tag(name = "游戏资料 - 进化条件")
class GameEvolutionDetailsController(
	service: GameEvolutionDetailsService,
) : GameDataCrudControllerSupport(service)
