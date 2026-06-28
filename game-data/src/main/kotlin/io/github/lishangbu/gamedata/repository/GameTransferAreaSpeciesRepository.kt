package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

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
 * 迁移区域种类持久化访问。
 */
@Repository
class GameTransferAreaSpeciesRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_TRANSFER_AREA_SPECIES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_TRANSFER_AREA_SPECIES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_TRANSFER_AREA_SPECIES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_TRANSFER_AREA_SPECIES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_TRANSFER_AREA_SPECIES_TABLE, id)
	}
}
