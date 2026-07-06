package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_LOCATION_AREA_ENCOUNTER_CONDITION_VALUES_TABLE = GameDataTableSpec(
	tableName = "game_location_area_encounter_condition_value",
	label = "区域遭遇条件绑定",
	columns = listOf(
		GameDataColumnSpec(name = "encounter_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "condition_value_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("encounter_id", "condition_value_id"),
)

/**
 * 区域遭遇条件绑定持久化访问。
 */
@Repository
class GameLocationAreaEncounterConditionValuesRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_LOCATION_AREA_ENCOUNTER_CONDITION_VALUES_TABLE) {
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
