package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_ITEM_ATTRIBUTES_TABLE = GameDataTableSpec(
	tableName = "game_item_attribute",
	label = "道具属性",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "description", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name", "description"),
)

/**
 * 道具属性持久化访问。
 */
@Repository
class GameItemAttributesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_ITEM_ATTRIBUTES_TABLE)
