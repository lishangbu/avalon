package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CREATURE_ELEMENT_TABLE = GameDataTableSpec(
	tableName = "game_creature_element",
	label = "生物属性绑定",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "element_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "slot_order", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("creature_id", "element_id"),
)

/**
 * 生物属性绑定 Service。
 */
@Service
class GameCreatureElementService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CREATURE_ELEMENT_TABLE)
