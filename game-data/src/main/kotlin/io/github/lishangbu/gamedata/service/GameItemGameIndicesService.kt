package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameItemGameIndicesRequest
import io.github.lishangbu.gamedata.dto.GameItemGameIndicesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameItemGameIndicesRepository
import org.springframework.stereotype.Service

/**
 * 道具索引 Service。
 */
@Service
class GameItemGameIndicesService(
	private val repository: GameItemGameIndicesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameItemGameIndicesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameItemGameIndicesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameItemGameIndicesResponse =
		GameItemGameIndicesResponse.from(repository.get(id))

	fun create(request: GameItemGameIndicesRequest): GameItemGameIndicesResponse =
		GameItemGameIndicesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameItemGameIndicesRequest): GameItemGameIndicesResponse =
		GameItemGameIndicesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameItemGameIndicesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
