package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_GROWTH_RATE_LEVELS_TABLE = GameDataTableSpec(
	tableName = "game_growth_rate_level",
	label = "成长等级经验",
	columns = listOf(
		GameDataColumnSpec(name = "growth_rate_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "level", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "experience", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("growth_rate_id", "level"),
)

/**
 * 成长等级经验持久化访问。
 */
@Repository
class GameGrowthRateLevelsRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_GROWTH_RATE_LEVELS_TABLE) {
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
