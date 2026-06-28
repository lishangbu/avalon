package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCatalogsRequest
import io.github.lishangbu.gamedata.dto.GameCatalogsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCatalogsRepository
import org.springframework.stereotype.Service

/**
 * 图鉴目录 Service。
 */
@Service
class GameCatalogsService(
	private val repository: GameCatalogsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCatalogsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCatalogsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCatalogsResponse =
		GameCatalogsResponse.from(repository.get(id))

	fun create(request: GameCatalogsRequest): GameCatalogsResponse =
		GameCatalogsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCatalogsRequest): GameCatalogsResponse =
		GameCatalogsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCatalogsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
