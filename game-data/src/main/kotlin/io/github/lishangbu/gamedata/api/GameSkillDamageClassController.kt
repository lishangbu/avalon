package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSkillDamageClassService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 技能分类管理接口。
 */
@RestController
@RequestMapping("/api/game-data/skill-damage-classes")
@Tag(name = "游戏资料 - 技能分类")
class GameSkillDamageClassController(
	service: GameSkillDamageClassService,
) : GameDataCrudControllerSupport(service)
