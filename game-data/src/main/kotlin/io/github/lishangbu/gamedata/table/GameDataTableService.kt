package io.github.lishangbu.gamedata.table

import io.github.lishangbu.gamedata.api.GameDataPageResponse
import io.github.lishangbu.gamedata.api.GameDataRecordRequest
import io.github.lishangbu.gamedata.api.GameDataRecordResponse

/**
 * 每张资料表 Service 的公共委托基类。
 */
abstract class GameDataTableService(
	private val operations: GameDataJdbcOperations,
	private val table: GameDataTableSpec,
) {
	fun list(page: Int, size: Int, query: String?): GameDataPageResponse =
		operations.list(table, page, size, query)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(table, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(table, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(table, id, request)

	fun delete(id: Long) {
		operations.delete(table, id)
	}
}
