package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCharacteristicValuesRequest
import io.github.lishangbu.gamedata.dto.GameCharacteristicValuesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCharacteristicValuesRepository
import org.springframework.stereotype.Service

/**
 * 个体特征取值 Service。
 */
@Service
class GameCharacteristicValuesService(
	private val repository: GameCharacteristicValuesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCharacteristicValuesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCharacteristicValuesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCharacteristicValuesResponse =
		GameCharacteristicValuesResponse.from(repository.get(id))

	fun create(request: GameCharacteristicValuesRequest): GameCharacteristicValuesResponse =
		GameCharacteristicValuesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCharacteristicValuesRequest): GameCharacteristicValuesResponse =
		GameCharacteristicValuesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCharacteristicValuesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
