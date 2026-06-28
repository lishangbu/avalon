package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameItemDetailsRequest
import io.github.lishangbu.gamedata.dto.GameItemDetailsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameItemDetailsRepository
import org.springframework.stereotype.Service

/**
 * 道具详情 Service。
 */
@Service
class GameItemDetailsService(
	private val repository: GameItemDetailsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameItemDetailsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameItemDetailsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameItemDetailsResponse =
		GameItemDetailsResponse.from(repository.get(id))

	fun create(request: GameItemDetailsRequest): GameItemDetailsResponse =
		GameItemDetailsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameItemDetailsRequest): GameItemDetailsResponse =
		GameItemDetailsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameItemDetailsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
