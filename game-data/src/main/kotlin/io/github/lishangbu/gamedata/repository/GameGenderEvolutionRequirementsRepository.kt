package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

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
 * 性别进化要求持久化访问。
 */
@Repository
class GameGenderEvolutionRequirementsRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_GENDER_EVOLUTION_REQUIREMENTS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_GENDER_EVOLUTION_REQUIREMENTS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_GENDER_EVOLUTION_REQUIREMENTS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_GENDER_EVOLUTION_REQUIREMENTS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_GENDER_EVOLUTION_REQUIREMENTS_TABLE, id)
	}
}
