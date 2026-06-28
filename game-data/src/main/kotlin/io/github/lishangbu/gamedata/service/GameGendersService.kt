package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameGendersRequest
import io.github.lishangbu.gamedata.dto.GameGendersResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameGendersRepository
import org.springframework.stereotype.Service

/**
 * 性别资料 Service。
 */
@Service
class GameGendersService(
	private val repository: GameGendersRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameGendersResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameGendersResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameGendersResponse =
		GameGendersResponse.from(repository.get(id))

	fun create(request: GameGendersRequest): GameGendersResponse =
		GameGendersResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameGendersRequest): GameGendersResponse =
		GameGendersResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameGendersRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
