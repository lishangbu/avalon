package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameLocationAreasRequest
import io.github.lishangbu.gamedata.dto.GameLocationAreasResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameLocationAreasRepository
import org.springframework.stereotype.Service

/**
 * 地点区域 Service。
 */
@Service
class GameLocationAreasService(
	private val repository: GameLocationAreasRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameLocationAreasResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameLocationAreasResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameLocationAreasResponse =
		GameLocationAreasResponse.from(repository.get(id))

	fun create(request: GameLocationAreasRequest): GameLocationAreasResponse =
		GameLocationAreasResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameLocationAreasRequest): GameLocationAreasResponse =
		GameLocationAreasResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameLocationAreasRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
