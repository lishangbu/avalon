package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameNaturesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 性格资料管理接口。
 */
@RestController
@RequestMapping("/api/game-data/natures")
@Tag(name = "游戏资料 - 性格资料")
class GameNaturesController(
	service: GameNaturesService,
) : GameDataCrudControllerSupport(service)
