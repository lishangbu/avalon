package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameTransferAreasService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 迁移区域管理接口。
 */
@RestController
@RequestMapping("/api/game-data/transfer-areas")
@Tag(name = "游戏资料 - 迁移区域")
class GameTransferAreasController(
	service: GameTransferAreasService,
) : GameDataCrudControllerSupport(service)
