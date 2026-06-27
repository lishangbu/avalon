package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameCreatureAbilityService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 生物特性绑定管理接口。
 */
@RestController
@RequestMapping("/api/game-data/creature-abilities")
@Tag(name = "游戏资料 - 生物特性绑定")
class GameCreatureAbilityController(
	service: GameCreatureAbilityService,
) : GameDataCrudControllerSupport(service)
