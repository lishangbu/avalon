package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameLocationGameIndicesRequest
import io.github.lishangbu.gamedata.dto.GameLocationGameIndicesResponse
import io.github.lishangbu.gamedata.entity.GameLocationGameIndices
import io.github.lishangbu.gamedata.entity.gameIndex
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.locationId
import io.github.lishangbu.gamedata.repository.GameLocationGameIndicesRepository
import io.github.lishangbu.gamedata.support.gameDataBooleanFilterValue
import io.github.lishangbu.gamedata.support.gameDataIntFilterValue
import io.github.lishangbu.gamedata.support.gameDataLongFilterValue
import io.github.lishangbu.gamedata.support.gameDataOptionalText
import io.github.lishangbu.gamedata.support.gameDataRequiredText
import io.github.lishangbu.gamedata.support.gameDataStringFilterValue
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.sql
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 地点索引维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameLocationGameIndicesService(
	private val repository: GameLocationGameIndicesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameLocationGameIndicesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameLocationGameIndices::class) {
			search.pattern?.let { pattern ->
				where(sql<String>("cast(%e as text)", table.locationId) ilike pattern)
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"location_id" -> gameDataLongFilterValue("location_id", rawValue)?.let { where(table.locationId eq it) }
				"game_index" -> gameDataIntFilterValue("game_index", rawValue)?.let { where(table.gameIndex eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameLocationGameIndicesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameLocationGameIndicesRequest): GameLocationGameIndicesResponse =
		repository.save(
			GameLocationGameIndices {
				locationId = request.locationId ?: invalidValue("location_id", "location_id 不能为空")
				gameIndex = request.gameIndex ?: invalidValue("game_index", "game_index 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameLocationGameIndicesRequest): GameLocationGameIndicesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameLocationGameIndices {
				this.id = id
				locationId = request.locationId ?: invalidValue("location_id", "location_id 不能为空")
				gameIndex = request.gameIndex ?: invalidValue("game_index", "game_index 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameLocationGameIndices =
		repository.findNullable(id) ?: notFound("id", "地点索引不存在: $id")

	private fun GameLocationGameIndices.toResponse(): GameLocationGameIndicesResponse =
		GameLocationGameIndicesResponse {
			id = this@toResponse.id
			locationId = this@toResponse.locationId
			gameIndex = this@toResponse.gameIndex
		}
}
