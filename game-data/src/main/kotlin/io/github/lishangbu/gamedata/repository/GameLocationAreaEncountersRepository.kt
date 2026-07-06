package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_LOCATION_AREA_ENCOUNTERS_TABLE = GameDataTableSpec(
	tableName = "game_location_area_encounter",
	label = "区域精灵遭遇",
	columns = listOf(
		GameDataColumnSpec(name = "area_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "method_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "min_level", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "max_level", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "chance", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "max_chance", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("area_id", "creature_id"),
)

/**
 * 区域精灵遭遇持久化访问。
 */
@Repository
class GameLocationAreaEncountersRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_LOCATION_AREA_ENCOUNTERS_TABLE) {
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
