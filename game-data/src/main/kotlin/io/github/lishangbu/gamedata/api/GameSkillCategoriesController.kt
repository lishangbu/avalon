package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSkillCategoriesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 技能元分类管理接口。
 */
@RestController
@RequestMapping("/api/game-data/skill-categories")
@Tag(name = "游戏资料 - 技能元分类")
class GameSkillCategoriesController(
	service: GameSkillCategoriesService,
) : GameDataCrudControllerSupport(service)
