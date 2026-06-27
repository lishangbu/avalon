package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameGenderEvolutionRequirementsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 性别进化要求管理接口。
 */
@RestController
@RequestMapping("/api/game-data/gender-evolution-requirements")
@Tag(name = "游戏资料 - 性别进化要求")
class GameGenderEvolutionRequirementsController(
	service: GameGenderEvolutionRequirementsService,
) : GameDataCrudControllerSupport(service)
