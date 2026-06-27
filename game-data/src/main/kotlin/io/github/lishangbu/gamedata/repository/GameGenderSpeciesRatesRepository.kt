package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

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
 * 性别种类比例持久化访问。
 */
@Repository
class GameGenderSpeciesRatesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_GENDER_SPECIES_RATES_TABLE)
