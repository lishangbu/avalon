package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameTransferAreaSpeciesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 迁移区域种类管理接口。
 */
@RestController
@RequestMapping("/api/game-data/transfer-area-species")
@Tag(name = "游戏资料 - 迁移区域种类")
class GameTransferAreaSpeciesController(
	service: GameTransferAreaSpeciesService,
) : GameDataCrudControllerSupport(service)
