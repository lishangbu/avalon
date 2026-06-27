package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameEventStatNatureEffectsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 活动能力性格影响管理接口。
 */
@RestController
@RequestMapping("/api/game-data/event-stat-nature-effects")
@Tag(name = "游戏资料 - 活动能力性格影响")
class GameEventStatNatureEffectsController(
	service: GameEventStatNatureEffectsService,
) : GameDataCrudControllerSupport(service)
