package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameEncounterConditionsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 遭遇条件管理接口。
 */
@RestController
@RequestMapping("/api/game-data/encounter-conditions")
@Tag(name = "游戏资料 - 遭遇条件")
class GameEncounterConditionsController(
	service: GameEncounterConditionsService,
) : GameDataCrudControllerSupport(service)
