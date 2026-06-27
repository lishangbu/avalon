package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameItemAttributeBindingsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 道具属性绑定管理接口。
 */
@RestController
@RequestMapping("/api/game-data/item-attribute-bindings")
@Tag(name = "游戏资料 - 道具属性绑定")
class GameItemAttributeBindingsController(
	service: GameItemAttributeBindingsService,
) : GameDataCrudControllerSupport(service)
