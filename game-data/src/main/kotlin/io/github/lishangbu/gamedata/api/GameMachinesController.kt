package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameMachinesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 机器资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/machines")
@Tag(name = "游戏资料 - 机器资料")
class GameMachinesController(
	service: GameMachinesService,
) : GameDataCrudControllerSupport(service)
