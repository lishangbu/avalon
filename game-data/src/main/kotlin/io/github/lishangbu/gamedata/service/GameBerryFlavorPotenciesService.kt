package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameBerryFlavorPotenciesRequest
import io.github.lishangbu.gamedata.dto.GameBerryFlavorPotenciesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameBerryFlavorPotenciesRepository
import org.springframework.stereotype.Service

/**
 * 树果口味强度 Service。
 */
@Service
class GameBerryFlavorPotenciesService(
	private val repository: GameBerryFlavorPotenciesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameBerryFlavorPotenciesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameBerryFlavorPotenciesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameBerryFlavorPotenciesResponse =
		GameBerryFlavorPotenciesResponse.from(repository.get(id))

	fun create(request: GameBerryFlavorPotenciesRequest): GameBerryFlavorPotenciesResponse =
		GameBerryFlavorPotenciesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameBerryFlavorPotenciesRequest): GameBerryFlavorPotenciesResponse =
		GameBerryFlavorPotenciesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameBerryFlavorPotenciesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
