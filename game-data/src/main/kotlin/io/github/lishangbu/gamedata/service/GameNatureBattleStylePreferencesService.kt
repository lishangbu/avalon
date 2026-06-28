package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameNatureBattleStylePreferencesRequest
import io.github.lishangbu.gamedata.dto.GameNatureBattleStylePreferencesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameNatureBattleStylePreferencesRepository
import org.springframework.stereotype.Service

/**
 * 性格战斗风格偏好 Service。
 */
@Service
class GameNatureBattleStylePreferencesService(
	private val repository: GameNatureBattleStylePreferencesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameNatureBattleStylePreferencesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameNatureBattleStylePreferencesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameNatureBattleStylePreferencesResponse =
		GameNatureBattleStylePreferencesResponse.from(repository.get(id))

	fun create(request: GameNatureBattleStylePreferencesRequest): GameNatureBattleStylePreferencesResponse =
		GameNatureBattleStylePreferencesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameNatureBattleStylePreferencesRequest): GameNatureBattleStylePreferencesResponse =
		GameNatureBattleStylePreferencesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameNatureBattleStylePreferencesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
