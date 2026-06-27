package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_GENDER_SPECIES_RATES_TABLE = GameDataTableSpec(
	tableName = "game_gender_species_rate",
	label = "性别种类比例",
	columns = listOf(
		GameDataColumnSpec(name = "gender_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "rate", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("gender_id", "species_id"),
)

/**
 * 性别种类比例 Service。
 */
@Service
class GameGenderSpeciesRatesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_GENDER_SPECIES_RATES_TABLE)
