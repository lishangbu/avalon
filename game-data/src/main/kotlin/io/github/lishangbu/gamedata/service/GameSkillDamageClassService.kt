package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSkillDamageClassRequest
import io.github.lishangbu.gamedata.dto.GameSkillDamageClassResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSkillDamageClassRepository
import org.springframework.stereotype.Service

/**
 * 技能分类 Service。
 */
@Service
class GameSkillDamageClassService(
	private val repository: GameSkillDamageClassRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSkillDamageClassResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSkillDamageClassResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSkillDamageClassResponse =
		GameSkillDamageClassResponse.from(repository.get(id))

	fun create(request: GameSkillDamageClassRequest): GameSkillDamageClassResponse =
		GameSkillDamageClassResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSkillDamageClassRequest): GameSkillDamageClassResponse =
		GameSkillDamageClassResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSkillDamageClassRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
