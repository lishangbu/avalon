package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameEncounterMethodsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 遭遇方式管理接口。
 */
@RestController
@RequestMapping("/api/game-data/encounter-methods")
@Tag(name = "游戏资料 - 遭遇方式")
class GameEncounterMethodsController(
	service: GameEncounterMethodsService,
) : GameDataCrudControllerSupport(service)
