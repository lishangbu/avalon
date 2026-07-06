package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_ITEM_CATEGORY_POCKETS_TABLE = GameDataTableSpec(
	tableName = "game_item_category_pocket",
	label = "道具分类口袋",
	columns = listOf(
		GameDataColumnSpec(name = "category_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "pocket_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("category_id", "pocket_id"),
)

/**
 * 道具分类口袋持久化访问。
 */
@Repository
class GameItemCategoryPocketsRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_ITEM_CATEGORY_POCKETS_TABLE) {
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
