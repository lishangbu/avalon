package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameCatalogVersionGroupsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 目录版本组绑定管理接口。
 */
@RestController
@RequestMapping("/api/game-data/catalog-version-groups")
@Tag(name = "游戏资料 - 目录版本组绑定")
class GameCatalogVersionGroupsController(
	service: GameCatalogVersionGroupsService,
) : GameDataCrudControllerSupport(service)
