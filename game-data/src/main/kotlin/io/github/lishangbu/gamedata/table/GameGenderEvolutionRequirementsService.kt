package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_GENDER_EVOLUTION_REQUIREMENTS_TABLE = GameDataTableSpec(
	tableName = "game_gender_evolution_requirement",
	label = "性别进化要求",
	columns = listOf(
		GameDataColumnSpec(name = "gender_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("gender_id", "species_id"),
)

/**
 * 性别进化要求 Service。
 */
@Service
class GameGenderEvolutionRequirementsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_GENDER_EVOLUTION_REQUIREMENTS_TABLE)
