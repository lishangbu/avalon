package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameCreatureStatService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 生物数值绑定管理接口。
 */
@RestController
@RequestMapping("/api/game-data/creature-stats")
@Tag(name = "游戏资料 - 生物数值绑定")
class GameCreatureStatController(
	service: GameCreatureStatService,
) : GameDataCrudControllerSupport(service)
