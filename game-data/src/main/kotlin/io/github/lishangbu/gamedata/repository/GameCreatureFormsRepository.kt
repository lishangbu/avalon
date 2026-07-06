package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_CREATURE_FORMS_TABLE = GameDataTableSpec(
	tableName = "game_creature_form",
	label = "精灵形态",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "form_name", type = GameDataColumnType.STRING, maxLength = 120),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "form_order", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "battle_only", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "default_form", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "enhanced_form", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name", "form_name"),
)

/**
 * 精灵形态持久化访问。
 */
@Repository
class GameCreatureFormsRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_CREATURE_FORMS_TABLE) {
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
