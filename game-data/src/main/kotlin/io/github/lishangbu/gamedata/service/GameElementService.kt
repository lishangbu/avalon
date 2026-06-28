package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameElementRequest
import io.github.lishangbu.gamedata.dto.GameElementResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameElementRepository
import org.springframework.stereotype.Service

/**
 * 属性资料 Service。
 */
@Service
class GameElementService(
	private val repository: GameElementRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameElementResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameElementResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameElementResponse =
		GameElementResponse.from(repository.get(id))

	fun create(request: GameElementRequest): GameElementResponse =
		GameElementResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameElementRequest): GameElementResponse =
		GameElementResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameElementRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
