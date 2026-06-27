package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ELEMENT_DAMAGE_RELATIONS_TABLE = GameDataTableSpec(
	tableName = "game_element_damage_relation",
	label = "属性克制关系",
	columns = listOf(
		GameDataColumnSpec(name = "source_element_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "target_element_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "relation_type", type = GameDataColumnType.STRING, required = true, maxLength = 40),
		GameDataColumnSpec(name = "generation_id", type = GameDataColumnType.LONG),
	),
	searchColumns = listOf("source_element_id", "target_element_id", "relation_type"),
)

/**
 * 属性克制关系 Service。
 */
@Service
class GameElementDamageRelationsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ELEMENT_DAMAGE_RELATIONS_TABLE)
