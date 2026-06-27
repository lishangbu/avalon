package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameElementDamageRelationsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 属性克制关系管理接口。
 */
@RestController
@RequestMapping("/api/game-data/element-damage-relations")
@Tag(name = "游戏资料 - 属性克制关系")
class GameElementDamageRelationsController(
	service: GameElementDamageRelationsService,
) : GameDataCrudControllerSupport(service)
