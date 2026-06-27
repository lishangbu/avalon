package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameItemFlingEffectsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 道具投掷效果管理接口。
 */
@RestController
@RequestMapping("/api/game-data/item-fling-effects")
@Tag(name = "游戏资料 - 道具投掷效果")
class GameItemFlingEffectsController(
	service: GameItemFlingEffectsService,
) : GameDataCrudControllerSupport(service)
