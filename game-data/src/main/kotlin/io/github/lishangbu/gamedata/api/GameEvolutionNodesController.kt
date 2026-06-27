package io.github.lishangbu.gamedata.api

import io.github.lishangbu.gamedata.table.GameEvolutionNodesService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 进化链节点管理接口。
 */
@RestController
@RequestMapping("/api/game-data/evolution-nodes")
@Tag(name = "游戏资料 - 进化链节点")
class GameEvolutionNodesController(
	service: GameEvolutionNodesService,
) : GameDataCrudControllerSupport(service)
