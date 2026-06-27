package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSpeciesEggGroupService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 种类分组绑定管理接口。
 */
@RestController
@RequestMapping("/api/game-data/species-egg-groups")
@Tag(name = "游戏资料 - 种类分组绑定")
class GameSpeciesEggGroupController(
	service: GameSpeciesEggGroupService,
) : GameDataCrudControllerSupport(service)
