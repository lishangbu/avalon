package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameGenderSpeciesRatesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 性别种类比例管理接口。
 */
@RestController
@RequestMapping("/api/game-data/gender-species-rates")
@Tag(name = "游戏资料 - 性别种类比例")
class GameGenderSpeciesRatesController(
	service: GameGenderSpeciesRatesService,
) : GameDataCrudControllerSupport(service)
