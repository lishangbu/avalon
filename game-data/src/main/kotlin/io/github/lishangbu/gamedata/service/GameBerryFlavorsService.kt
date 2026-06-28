package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameBerryFlavorsRequest
import io.github.lishangbu.gamedata.dto.GameBerryFlavorsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameBerryFlavorsRepository
import org.springframework.stereotype.Service

/**
 * 树果口味 Service。
 */
@Service
class GameBerryFlavorsService(
	private val repository: GameBerryFlavorsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameBerryFlavorsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameBerryFlavorsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameBerryFlavorsResponse =
		GameBerryFlavorsResponse.from(repository.get(id))

	fun create(request: GameBerryFlavorsRequest): GameBerryFlavorsResponse =
		GameBerryFlavorsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameBerryFlavorsRequest): GameBerryFlavorsResponse =
		GameBerryFlavorsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameBerryFlavorsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
