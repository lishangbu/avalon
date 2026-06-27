package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_TRANSFER_AREA_SPECIES_TABLE = GameDataTableSpec(
	tableName = "game_transfer_area_species",
	label = "迁移区域种类",
	columns = listOf(
		GameDataColumnSpec(name = "area_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "base_score", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "rate", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("area_id", "species_id"),
)

/**
 * 迁移区域种类 Service。
 */
@Service
class GameTransferAreaSpeciesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_TRANSFER_AREA_SPECIES_TABLE)
