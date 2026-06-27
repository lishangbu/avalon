package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameCreatureFormsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 生物形态管理接口。
 */
@RestController
@RequestMapping("/api/game-data/creature-forms")
@Tag(name = "游戏资料 - 生物形态")
class GameCreatureFormsController(
	service: GameCreatureFormsService,
) : GameDataCrudControllerSupport(service)
