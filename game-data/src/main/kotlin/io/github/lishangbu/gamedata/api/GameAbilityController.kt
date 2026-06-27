package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameAbilityService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 特性资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/abilities")
@Tag(name = "游戏资料 - 特性资料")
class GameAbilityController(
	service: GameAbilityService,
) : GameDataCrudControllerSupport(service)
