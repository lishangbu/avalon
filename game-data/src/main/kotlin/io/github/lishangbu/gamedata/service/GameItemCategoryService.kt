package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameItemCategoryRequest
import io.github.lishangbu.gamedata.dto.GameItemCategoryResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameItemCategoryRepository
import org.springframework.stereotype.Service

/**
 * 道具分类 Service。
 */
@Service
class GameItemCategoryService(
	private val repository: GameItemCategoryRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameItemCategoryResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameItemCategoryResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameItemCategoryResponse =
		GameItemCategoryResponse.from(repository.get(id))

	fun create(request: GameItemCategoryRequest): GameItemCategoryResponse =
		GameItemCategoryResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameItemCategoryRequest): GameItemCategoryResponse =
		GameItemCategoryResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameItemCategoryRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
