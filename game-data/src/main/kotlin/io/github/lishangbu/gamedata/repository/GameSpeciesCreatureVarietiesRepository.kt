package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_SPECIES_CREATURE_VARIETIES_TABLE = GameDataTableSpec(
	tableName = "game_species_creature_variety",
	label = "种类生物变种",
	columns = listOf(
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "default_variety", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("species_id", "creature_id"),
)

/**
 * 种类生物变种持久化访问。
 */
@Repository
class GameSpeciesCreatureVarietiesRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_SPECIES_CREATURE_VARIETIES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_SPECIES_CREATURE_VARIETIES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_SPECIES_CREATURE_VARIETIES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_SPECIES_CREATURE_VARIETIES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_SPECIES_CREATURE_VARIETIES_TABLE, id)
	}
}
