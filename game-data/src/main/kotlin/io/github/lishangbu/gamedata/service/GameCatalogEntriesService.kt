package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCatalogEntriesRequest
import io.github.lishangbu.gamedata.dto.GameCatalogEntriesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCatalogEntriesRepository
import org.springframework.stereotype.Service

/**
 * 图鉴目录条目 Service。
 */
@Service
class GameCatalogEntriesService(
	private val repository: GameCatalogEntriesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCatalogEntriesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCatalogEntriesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCatalogEntriesResponse =
		GameCatalogEntriesResponse.from(repository.get(id))

	fun create(request: GameCatalogEntriesRequest): GameCatalogEntriesResponse =
		GameCatalogEntriesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCatalogEntriesRequest): GameCatalogEntriesResponse =
		GameCatalogEntriesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCatalogEntriesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
