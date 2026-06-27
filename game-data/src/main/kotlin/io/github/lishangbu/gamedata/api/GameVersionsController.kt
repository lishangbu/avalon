package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameVersionsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 版本资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/versions")
@Tag(name = "游戏资料 - 版本资料")
class GameVersionsController(
	service: GameVersionsService,
) : GameDataCrudControllerSupport(service)
