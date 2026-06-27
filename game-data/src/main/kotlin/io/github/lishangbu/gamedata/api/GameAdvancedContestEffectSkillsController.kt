package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameAdvancedContestEffectSkillsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 高级评价效果技能管理接口。
 */
@RestController
@RequestMapping("/api/game-data/advanced-contest-effect-skills")
@Tag(name = "游戏资料 - 高级评价效果技能")
class GameAdvancedContestEffectSkillsController(
	service: GameAdvancedContestEffectSkillsService,
) : GameDataCrudControllerSupport(service)
