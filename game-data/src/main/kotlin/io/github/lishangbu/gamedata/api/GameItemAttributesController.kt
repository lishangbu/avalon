package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameItemAttributesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 道具属性管理接口。
 */
@RestController
@RequestMapping("/api/game-data/item-attributes")
@Tag(name = "游戏资料 - 道具属性")
class GameItemAttributesController(
	service: GameItemAttributesService,
) : GameDataCrudControllerSupport(service)
