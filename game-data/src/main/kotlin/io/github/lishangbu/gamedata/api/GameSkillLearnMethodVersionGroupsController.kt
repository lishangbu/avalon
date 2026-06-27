package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameSkillLearnMethodVersionGroupsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 学习方式版本组管理接口。
 */
@RestController
@RequestMapping("/api/game-data/skill-learn-method-version-groups")
@Tag(name = "游戏资料 - 学习方式版本组")
class GameSkillLearnMethodVersionGroupsController(
	service: GameSkillLearnMethodVersionGroupsService,
) : GameDataCrudControllerSupport(service)
