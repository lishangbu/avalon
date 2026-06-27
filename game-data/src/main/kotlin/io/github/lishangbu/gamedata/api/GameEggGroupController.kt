package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameEggGroupService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 种类分组管理接口。
 */
@RestController
@RequestMapping("/api/game-data/egg-groups")
@Tag(name = "游戏资料 - 种类分组")
class GameEggGroupController(
	service: GameEggGroupService,
) : GameDataCrudControllerSupport(service)
