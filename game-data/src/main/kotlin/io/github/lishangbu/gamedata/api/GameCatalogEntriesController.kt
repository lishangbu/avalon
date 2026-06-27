package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameCatalogEntriesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 图鉴目录条目管理接口。
 */
@RestController
@RequestMapping("/api/game-data/catalog-entries")
@Tag(name = "游戏资料 - 图鉴目录条目")
class GameCatalogEntriesController(
	service: GameCatalogEntriesService,
) : GameDataCrudControllerSupport(service)
