package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameGrowthRateLevelsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 成长等级经验管理接口。
 */
@RestController
@RequestMapping("/api/game-data/growth-rate-levels")
@Tag(name = "游戏资料 - 成长等级经验")
class GameGrowthRateLevelsController(
	service: GameGrowthRateLevelsService,
) : GameDataCrudControllerSupport(service)
