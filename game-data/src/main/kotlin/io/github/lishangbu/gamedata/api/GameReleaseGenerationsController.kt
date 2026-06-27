package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameReleaseGenerationsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 发布代际管理接口。
 */
@RestController
@RequestMapping("/api/game-data/release-generations")
@Tag(name = "游戏资料 - 发布代际")
class GameReleaseGenerationsController(
	service: GameReleaseGenerationsService,
) : GameDataCrudControllerSupport(service)
