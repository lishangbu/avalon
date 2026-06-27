package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameLocationAreaEncountersService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 区域生物遭遇管理接口。
 */
@RestController
@RequestMapping("/api/game-data/location-area-encounters")
@Tag(name = "游戏资料 - 区域生物遭遇")
class GameLocationAreaEncountersController(
	service: GameLocationAreaEncountersService,
) : GameDataCrudControllerSupport(service)
