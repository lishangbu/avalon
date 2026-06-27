package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_RELEASE_GENERATIONS_TABLE = GameDataTableSpec(
	tableName = "game_release_generation",
	label = "发布代际",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 发布代际 Service。
 */
@Service
class GameReleaseGenerationsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_RELEASE_GENERATIONS_TABLE)
