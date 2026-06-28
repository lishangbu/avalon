package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameRegionsRequest
import io.github.lishangbu.gamedata.dto.GameRegionsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameRegionsRepository
import org.springframework.stereotype.Service

/**
 * 地区资料 Service。
 */
@Service
class GameRegionsService(
	private val repository: GameRegionsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameRegionsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameRegionsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameRegionsResponse =
		GameRegionsResponse.from(repository.get(id))

	fun create(request: GameRegionsRequest): GameRegionsResponse =
		GameRegionsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameRegionsRequest): GameRegionsResponse =
		GameRegionsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameRegionsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
