package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameContestEffectsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 评价效果管理接口。
 */
@RestController
@RequestMapping("/api/game-data/contest-effects")
@Tag(name = "游戏资料 - 评价效果")
class GameContestEffectsController(
	service: GameContestEffectsService,
) : GameDataCrudControllerSupport(service)
