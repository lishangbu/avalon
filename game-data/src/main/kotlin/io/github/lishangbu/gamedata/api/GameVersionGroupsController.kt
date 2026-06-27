package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameVersionGroupsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 版本组管理接口。
 */
@RestController
@RequestMapping("/api/game-data/version-groups")
@Tag(name = "游戏资料 - 版本组")
class GameVersionGroupsController(
	service: GameVersionGroupsService,
) : GameDataCrudControllerSupport(service)
