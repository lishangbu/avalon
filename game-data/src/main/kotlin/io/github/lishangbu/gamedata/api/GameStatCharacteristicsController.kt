package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameStatCharacteristicsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 数值项特征管理接口。
 */
@RestController
@RequestMapping("/api/game-data/stat-characteristics")
@Tag(name = "游戏资料 - 数值项特征")
class GameStatCharacteristicsController(
	service: GameStatCharacteristicsService,
) : GameDataCrudControllerSupport(service)
