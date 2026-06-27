package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameCharacteristicValuesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 个体特征取值管理接口。
 */
@RestController
@RequestMapping("/api/game-data/characteristic-values")
@Tag(name = "游戏资料 - 个体特征取值")
class GameCharacteristicValuesController(
	service: GameCharacteristicValuesService,
) : GameDataCrudControllerSupport(service)
