package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameEvolutionTriggersRequest
import io.github.lishangbu.gamedata.dto.GameEvolutionTriggersResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameEvolutionTriggersRepository
import org.springframework.stereotype.Service

/**
 * 进化触发器 Service。
 */
@Service
class GameEvolutionTriggersService(
	private val repository: GameEvolutionTriggersRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameEvolutionTriggersResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameEvolutionTriggersResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameEvolutionTriggersResponse =
		GameEvolutionTriggersResponse.from(repository.get(id))

	fun create(request: GameEvolutionTriggersRequest): GameEvolutionTriggersResponse =
		GameEvolutionTriggersResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameEvolutionTriggersRequest): GameEvolutionTriggersResponse =
		GameEvolutionTriggersResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameEvolutionTriggersRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
