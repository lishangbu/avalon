package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameItemAttributeBindingsRequest
import io.github.lishangbu.gamedata.dto.GameItemAttributeBindingsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameItemAttributeBindingsRepository
import org.springframework.stereotype.Service

/**
 * 道具属性绑定 Service。
 */
@Service
class GameItemAttributeBindingsService(
	private val repository: GameItemAttributeBindingsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameItemAttributeBindingsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameItemAttributeBindingsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameItemAttributeBindingsResponse =
		GameItemAttributeBindingsResponse.from(repository.get(id))

	fun create(request: GameItemAttributeBindingsRequest): GameItemAttributeBindingsResponse =
		GameItemAttributeBindingsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameItemAttributeBindingsRequest): GameItemAttributeBindingsResponse =
		GameItemAttributeBindingsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameItemAttributeBindingsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
