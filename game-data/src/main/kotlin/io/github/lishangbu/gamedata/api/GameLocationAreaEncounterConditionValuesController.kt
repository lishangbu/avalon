package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameLocationAreaEncounterConditionValuesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 区域遭遇条件绑定管理接口。
 */
@RestController
@RequestMapping("/api/game-data/location-area-encounter-condition-values")
@Tag(name = "游戏资料 - 区域遭遇条件绑定")
class GameLocationAreaEncounterConditionValuesController(
	service: GameLocationAreaEncounterConditionValuesService,
) : GameDataCrudControllerSupport(service)
