package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameGendersService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 性别资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/genders")
@Tag(name = "游戏资料 - 性别资料")
class GameGendersController(
	service: GameGendersService,
) : GameDataCrudControllerSupport(service)
