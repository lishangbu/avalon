package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncountersRequest
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncountersResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameLocationAreaEncountersRepository
import org.springframework.stereotype.Service

/**
 * 区域精灵遭遇 Service。
 */
@Service
class GameLocationAreaEncountersService(
	private val repository: GameLocationAreaEncountersRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameLocationAreaEncountersResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameLocationAreaEncountersResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameLocationAreaEncountersResponse =
		GameLocationAreaEncountersResponse.from(repository.get(id))

	fun create(request: GameLocationAreaEncountersRequest): GameLocationAreaEncountersResponse =
		GameLocationAreaEncountersResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameLocationAreaEncountersRequest): GameLocationAreaEncountersResponse =
		GameLocationAreaEncountersResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameLocationAreaEncountersRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
