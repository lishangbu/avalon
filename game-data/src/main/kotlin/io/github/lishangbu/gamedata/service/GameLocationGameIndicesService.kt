package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameLocationGameIndicesRequest
import io.github.lishangbu.gamedata.dto.GameLocationGameIndicesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameLocationGameIndicesRepository
import org.springframework.stereotype.Service

/**
 * 地点索引 Service。
 */
@Service
class GameLocationGameIndicesService(
	private val repository: GameLocationGameIndicesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameLocationGameIndicesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameLocationGameIndicesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameLocationGameIndicesResponse =
		GameLocationGameIndicesResponse.from(repository.get(id))

	fun create(request: GameLocationGameIndicesRequest): GameLocationGameIndicesResponse =
		GameLocationGameIndicesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameLocationGameIndicesRequest): GameLocationGameIndicesResponse =
		GameLocationGameIndicesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameLocationGameIndicesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
