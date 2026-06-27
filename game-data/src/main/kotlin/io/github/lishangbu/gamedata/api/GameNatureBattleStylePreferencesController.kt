package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameNatureBattleStylePreferencesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 性格战斗风格偏好管理接口。
 */
@RestController
@RequestMapping("/api/game-data/nature-battle-style-preferences")
@Tag(name = "游戏资料 - 性格战斗风格偏好")
class GameNatureBattleStylePreferencesController(
	service: GameNatureBattleStylePreferencesService,
) : GameDataCrudControllerSupport(service)
