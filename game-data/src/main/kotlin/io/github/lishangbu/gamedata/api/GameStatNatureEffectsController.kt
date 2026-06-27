package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameStatNatureEffectsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 数值项性格影响管理接口。
 */
@RestController
@RequestMapping("/api/game-data/stat-nature-effects")
@Tag(name = "游戏资料 - 数值项性格影响")
class GameStatNatureEffectsController(
	service: GameStatNatureEffectsService,
) : GameDataCrudControllerSupport(service)
