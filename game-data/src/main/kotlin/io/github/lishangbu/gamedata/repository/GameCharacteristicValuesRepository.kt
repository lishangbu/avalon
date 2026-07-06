package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_CHARACTERISTIC_VALUES_TABLE = GameDataTableSpec(
	tableName = "game_characteristic_value",
	label = "个体特征取值",
	columns = listOf(
		GameDataColumnSpec(name = "characteristic_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "possible_value", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("characteristic_id", "possible_value"),
)

/**
 * 个体特征取值持久化访问。
 */
@Repository
class GameCharacteristicValuesRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_CHARACTERISTIC_VALUES_TABLE) {
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
