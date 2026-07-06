package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_SPECIES_TABLE = GameDataTableSpec(
	tableName = "game_species",
	label = "种类资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "color_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "shape_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "habitat_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "gender_rate", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "capture_rate", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "base_happiness", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "hatch_counter", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "baby", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "legendary", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "mythical", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 种类资料持久化访问。
 */
@Repository
class GameSpeciesRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_SPECIES_TABLE) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		listRecords(page, size, query, filters)

	@Transactional(readOnly = true)
	fun get(id: Long): GameDataRecordResponse =
		getRecord(id)

	@Transactional
	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		createRecord(request)

	@Transactional
	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		updateRecord(id, request)

	@Transactional
	fun delete(id: Long) {
		deleteRecord(id)
	}
}
