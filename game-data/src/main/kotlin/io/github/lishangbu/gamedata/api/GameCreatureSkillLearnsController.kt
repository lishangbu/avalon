package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameCreatureSkillLearnsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 生物技能学习管理接口。
 */
@RestController
@RequestMapping("/api/game-data/creature-skill-learns")
@Tag(name = "游戏资料 - 生物技能学习")
class GameCreatureSkillLearnsController(
	service: GameCreatureSkillLearnsService,
) : GameDataCrudControllerSupport(service)
