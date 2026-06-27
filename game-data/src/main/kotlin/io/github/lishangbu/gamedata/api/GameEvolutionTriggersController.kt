package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameEvolutionTriggersService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 进化触发器管理接口。
 */
@RestController
@RequestMapping("/api/game-data/evolution-triggers")
@Tag(name = "游戏资料 - 进化触发器")
class GameEvolutionTriggersController(
	service: GameEvolutionTriggersService,
) : GameDataCrudControllerSupport(service)
