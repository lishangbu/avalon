package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameItemDetailsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 道具详情管理接口。
 */
@RestController
@RequestMapping("/api/game-data/item-details")
@Tag(name = "游戏资料 - 道具详情")
class GameItemDetailsController(
	service: GameItemDetailsService,
) : GameDataCrudControllerSupport(service)
