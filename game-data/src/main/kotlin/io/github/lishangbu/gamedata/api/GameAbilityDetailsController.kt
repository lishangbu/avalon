package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameAbilityDetailsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 特性详情管理接口。
 */
@RestController
@RequestMapping("/api/game-data/ability-details")
@Tag(name = "游戏资料 - 特性详情")
class GameAbilityDetailsController(
	service: GameAbilityDetailsService,
) : GameDataCrudControllerSupport(service)
