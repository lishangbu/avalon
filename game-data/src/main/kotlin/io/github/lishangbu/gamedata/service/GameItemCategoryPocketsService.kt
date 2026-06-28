package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameItemCategoryPocketsRequest
import io.github.lishangbu.gamedata.dto.GameItemCategoryPocketsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameItemCategoryPocketsRepository
import org.springframework.stereotype.Service

/**
 * 道具分类口袋 Service。
 */
@Service
class GameItemCategoryPocketsService(
	private val repository: GameItemCategoryPocketsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameItemCategoryPocketsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameItemCategoryPocketsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameItemCategoryPocketsResponse =
		GameItemCategoryPocketsResponse.from(repository.get(id))

	fun create(request: GameItemCategoryPocketsRequest): GameItemCategoryPocketsResponse =
		GameItemCategoryPocketsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameItemCategoryPocketsRequest): GameItemCategoryPocketsResponse =
		GameItemCategoryPocketsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameItemCategoryPocketsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
