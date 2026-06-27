package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameDataWriteRequest
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.repository.GameDataTableRepository

/**
 * 每张资料表 Service 的公共委托基类。
 *
 * Service 拥有接口 DTO 映射，具体表结构和字段白名单下沉到对应 Repository。
 */
abstract class GameDataTableService<REQUEST, RESPONSE>(
	private val repository: GameDataTableRepository,
	private val responseMapper: (GameDataRecordResponse) -> RESPONSE,
) where REQUEST : GameDataWriteRequest {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<RESPONSE> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(responseMapper),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): RESPONSE =
		responseMapper(repository.get(id))

	fun create(request: REQUEST): RESPONSE =
		responseMapper(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: REQUEST): RESPONSE =
		responseMapper(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun REQUEST.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
