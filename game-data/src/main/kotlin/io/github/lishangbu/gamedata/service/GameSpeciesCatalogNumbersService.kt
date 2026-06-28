package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSpeciesCatalogNumbersRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesCatalogNumbersResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSpeciesCatalogNumbersRepository
import org.springframework.stereotype.Service

/**
 * 种类目录编号 Service。
 */
@Service
class GameSpeciesCatalogNumbersService(
	private val repository: GameSpeciesCatalogNumbersRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSpeciesCatalogNumbersResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSpeciesCatalogNumbersResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSpeciesCatalogNumbersResponse =
		GameSpeciesCatalogNumbersResponse.from(repository.get(id))

	fun create(request: GameSpeciesCatalogNumbersRequest): GameSpeciesCatalogNumbersResponse =
		GameSpeciesCatalogNumbersResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSpeciesCatalogNumbersRequest): GameSpeciesCatalogNumbersResponse =
		GameSpeciesCatalogNumbersResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSpeciesCatalogNumbersRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
