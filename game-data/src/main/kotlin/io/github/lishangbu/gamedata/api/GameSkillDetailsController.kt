package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSkillDetailsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 技能详情管理接口。
 */
@RestController
@RequestMapping("/api/game-data/skill-details")
@Tag(name = "游戏资料 - 技能详情")
class GameSkillDetailsController(
	service: GameSkillDetailsService,
) : GameDataCrudControllerSupport(service)
