package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse

/**
 * 单张资料表持久化入口的公共委托基类。
 *
 * 每个资源拥有独立 Repository 和表白名单，本基类只复用底层 JDBC 执行流程。
 */
abstract class GameDataTableRepository(
	private val operations: GameDataJdbcOperations,
	private val table: GameDataTableSpec,
) {
	open fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(table, page, size, query, filters)

	open fun get(id: Long): GameDataRecordResponse =
		operations.get(table, id)

	open fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(table, request)

	open fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(table, id, request)

	open fun delete(id: Long) {
		operations.delete(table, id)
	}
}
