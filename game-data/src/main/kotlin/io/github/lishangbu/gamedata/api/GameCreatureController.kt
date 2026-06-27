package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameCreatureService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 生物资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/creatures")
@Tag(name = "游戏资料 - 生物资料")
class GameCreatureController(
	service: GameCreatureService,
) : GameDataCrudControllerSupport(service)
