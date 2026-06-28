package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameContestTypesRequest
import io.github.lishangbu.gamedata.dto.GameContestTypesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameContestTypesRepository
import org.springframework.stereotype.Service

/**
 * 评分类别 Service。
 */
@Service
class GameContestTypesService(
	private val repository: GameContestTypesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameContestTypesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameContestTypesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameContestTypesResponse =
		GameContestTypesResponse.from(repository.get(id))

	fun create(request: GameContestTypesRequest): GameContestTypesResponse =
		GameContestTypesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameContestTypesRequest): GameContestTypesResponse =
		GameContestTypesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameContestTypesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
