package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameCreatureHeldItemsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 生物持有道具管理接口。
 */
@RestController
@RequestMapping("/api/game-data/creature-held-items")
@Tag(name = "游戏资料 - 生物持有道具")
class GameCreatureHeldItemsController(
	service: GameCreatureHeldItemsService,
) : GameDataCrudControllerSupport(service)
