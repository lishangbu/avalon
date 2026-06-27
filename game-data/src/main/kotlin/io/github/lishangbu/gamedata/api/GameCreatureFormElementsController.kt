package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameCreatureFormElementsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 生物形态属性管理接口。
 */
@RestController
@RequestMapping("/api/game-data/creature-form-elements")
@Tag(name = "游戏资料 - 生物形态属性")
class GameCreatureFormElementsController(
	service: GameCreatureFormElementsService,
) : GameDataCrudControllerSupport(service)
