package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSkillCategoriesRequest
import io.github.lishangbu.gamedata.dto.GameSkillCategoriesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSkillCategoriesRepository
import org.springframework.stereotype.Service

/**
 * 技能元分类 Service。
 */
@Service
class GameSkillCategoriesService(
	private val repository: GameSkillCategoriesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSkillCategoriesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSkillCategoriesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSkillCategoriesResponse =
		GameSkillCategoriesResponse.from(repository.get(id))

	fun create(request: GameSkillCategoriesRequest): GameSkillCategoriesResponse =
		GameSkillCategoriesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSkillCategoriesRequest): GameSkillCategoriesResponse =
		GameSkillCategoriesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSkillCategoriesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
