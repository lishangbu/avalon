package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_EGG_GROUP_TABLE = GameDataTableSpec(
	tableName = "game_egg_group",
	label = "种类分组",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 种类分组持久化访问。
 */
@Repository
class GameEggGroupRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_EGG_GROUP_TABLE)
