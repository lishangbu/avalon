package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameGrowthRateLevelsRequest
import io.github.lishangbu.gamedata.dto.GameGrowthRateLevelsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameGrowthRateLevelsRepository
import org.springframework.stereotype.Service

/**
 * 成长等级经验 Service。
 */
@Service
class GameGrowthRateLevelsService(
	private val repository: GameGrowthRateLevelsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameGrowthRateLevelsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameGrowthRateLevelsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameGrowthRateLevelsResponse =
		GameGrowthRateLevelsResponse.from(repository.get(id))

	fun create(request: GameGrowthRateLevelsRequest): GameGrowthRateLevelsResponse =
		GameGrowthRateLevelsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameGrowthRateLevelsRequest): GameGrowthRateLevelsResponse =
		GameGrowthRateLevelsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameGrowthRateLevelsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
