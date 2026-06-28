package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameBerriesRequest
import io.github.lishangbu.gamedata.dto.GameBerriesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameBerriesRepository
import org.springframework.stereotype.Service

/**
 * 树果资料 Service。
 */
@Service
class GameBerriesService(
	private val repository: GameBerriesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameBerriesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameBerriesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameBerriesResponse =
		GameBerriesResponse.from(repository.get(id))

	fun create(request: GameBerriesRequest): GameBerriesResponse =
		GameBerriesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameBerriesRequest): GameBerriesResponse =
		GameBerriesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameBerriesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
