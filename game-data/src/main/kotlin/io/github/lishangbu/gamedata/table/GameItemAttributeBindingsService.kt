package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ITEM_ATTRIBUTE_BINDINGS_TABLE = GameDataTableSpec(
	tableName = "game_item_attribute_binding",
	label = "道具属性绑定",
	columns = listOf(
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "attribute_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("item_id", "attribute_id"),
)

/**
 * 道具属性绑定 Service。
 */
@Service
class GameItemAttributeBindingsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ITEM_ATTRIBUTE_BINDINGS_TABLE)
