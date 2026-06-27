package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameItemCategoryPocketsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 道具分类口袋管理接口。
 */
@RestController
@RequestMapping("/api/game-data/item-category-pockets")
@Tag(name = "游戏资料 - 道具分类口袋")
class GameItemCategoryPocketsController(
	service: GameItemCategoryPocketsService,
) : GameDataCrudControllerSupport(service)
