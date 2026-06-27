package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameContestTypesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 评分类别管理接口。
 */
@RestController
@RequestMapping("/api/game-data/contest-types")
@Tag(name = "游戏资料 - 评分类别")
class GameContestTypesController(
	service: GameContestTypesService,
) : GameDataCrudControllerSupport(service)
