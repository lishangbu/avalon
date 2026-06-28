package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
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
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_GENDER_SPECIES_RATES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_GENDER_SPECIES_RATES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_GENDER_SPECIES_RATES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_GENDER_SPECIES_RATES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_GENDER_SPECIES_RATES_TABLE, id)
	}
}
