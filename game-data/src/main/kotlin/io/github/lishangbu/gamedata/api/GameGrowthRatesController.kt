package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameGrowthRatesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 成长速率管理接口。
 */
@RestController
@RequestMapping("/api/game-data/growth-rates")
@Tag(name = "游戏资料 - 成长速率")
class GameGrowthRatesController(
	service: GameGrowthRatesService,
) : GameDataCrudControllerSupport(service)
