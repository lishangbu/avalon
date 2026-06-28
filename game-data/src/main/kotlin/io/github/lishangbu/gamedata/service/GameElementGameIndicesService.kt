package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameElementGameIndicesRequest
import io.github.lishangbu.gamedata.dto.GameElementGameIndicesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameElementGameIndicesRepository
import org.springframework.stereotype.Service

/**
 * 属性索引 Service。
 */
@Service
class GameElementGameIndicesService(
	private val repository: GameElementGameIndicesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameElementGameIndicesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameElementGameIndicesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameElementGameIndicesResponse =
		GameElementGameIndicesResponse.from(repository.get(id))

	fun create(request: GameElementGameIndicesRequest): GameElementGameIndicesResponse =
		GameElementGameIndicesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameElementGameIndicesRequest): GameElementGameIndicesResponse =
		GameElementGameIndicesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameElementGameIndicesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
