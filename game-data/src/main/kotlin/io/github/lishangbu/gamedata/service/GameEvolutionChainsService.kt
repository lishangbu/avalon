package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameEvolutionChainsRequest
import io.github.lishangbu.gamedata.dto.GameEvolutionChainsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameEvolutionChainsRepository
import org.springframework.stereotype.Service

/**
 * 进化链 Service。
 */
@Service
class GameEvolutionChainsService(
	private val repository: GameEvolutionChainsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameEvolutionChainsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameEvolutionChainsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameEvolutionChainsResponse =
		GameEvolutionChainsResponse.from(repository.get(id))

	fun create(request: GameEvolutionChainsRequest): GameEvolutionChainsResponse =
		GameEvolutionChainsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameEvolutionChainsRequest): GameEvolutionChainsResponse =
		GameEvolutionChainsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameEvolutionChainsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
