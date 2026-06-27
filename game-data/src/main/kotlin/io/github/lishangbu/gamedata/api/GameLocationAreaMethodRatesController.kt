package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameLocationAreaMethodRatesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 区域遭遇方式概率管理接口。
 */
@RestController
@RequestMapping("/api/game-data/location-area-method-rates")
@Tag(name = "游戏资料 - 区域遭遇方式概率")
class GameLocationAreaMethodRatesController(
	service: GameLocationAreaMethodRatesService,
) : GameDataCrudControllerSupport(service)
