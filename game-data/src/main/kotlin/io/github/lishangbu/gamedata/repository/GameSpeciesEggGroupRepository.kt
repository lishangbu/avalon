package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_SPECIES_EGG_GROUP_TABLE = GameDataTableSpec(
	tableName = "game_species_egg_group",
	label = "种类分组绑定",
	columns = listOf(
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "egg_group_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "slot_order", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("species_id", "egg_group_id"),
)

/**
 * 种类分组绑定持久化访问。
 */
@Repository
class GameSpeciesEggGroupRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_SPECIES_EGG_GROUP_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_SPECIES_EGG_GROUP_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_SPECIES_EGG_GROUP_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_SPECIES_EGG_GROUP_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_SPECIES_EGG_GROUP_TABLE, id)
	}
}
