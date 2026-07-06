package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_LOCATION_GAME_INDICES_TABLE = GameDataTableSpec(
	tableName = "game_location_game_index",
	label = "地点索引",
	columns = listOf(
		GameDataColumnSpec(name = "location_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "game_index", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("location_id"),
)

/**
 * 地点索引持久化访问。
 */
@Repository
class GameLocationGameIndicesRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_LOCATION_GAME_INDICES_TABLE) {
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
