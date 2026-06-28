package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameBerryFirmnessesRequest
import io.github.lishangbu.gamedata.dto.GameBerryFirmnessesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameBerryFirmnessesRepository
import org.springframework.stereotype.Service

/**
 * 树果硬度 Service。
 */
@Service
class GameBerryFirmnessesService(
	private val repository: GameBerryFirmnessesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameBerryFirmnessesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameBerryFirmnessesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameBerryFirmnessesResponse =
		GameBerryFirmnessesResponse.from(repository.get(id))

	fun create(request: GameBerryFirmnessesRequest): GameBerryFirmnessesResponse =
		GameBerryFirmnessesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameBerryFirmnessesRequest): GameBerryFirmnessesResponse =
		GameBerryFirmnessesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameBerryFirmnessesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
