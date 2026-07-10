package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameNatureEventStatChangesRequest
import io.github.lishangbu.gamedata.dto.GameNatureEventStatChangesResponse
import io.github.lishangbu.gamedata.entity.GameNatureEventStatChanges
import io.github.lishangbu.gamedata.entity.eventStatId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.maxChange
import io.github.lishangbu.gamedata.entity.natureId
import io.github.lishangbu.gamedata.repository.GameNatureEventStatChangesRepository
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
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.sql
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 性格活动能力变化维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameNatureEventStatChangesService(
	private val repository: GameNatureEventStatChangesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameNatureEventStatChangesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameNatureEventStatChanges::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.natureId) ilike pattern, sql<String>("cast(%e as text)", table.eventStatId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"nature_id" -> gameDataLongFilterValue("nature_id", rawValue)?.let { where(table.natureId eq it) }
				"event_stat_id" -> gameDataLongFilterValue("event_stat_id", rawValue)?.let { where(table.eventStatId eq it) }
				"max_change" -> gameDataIntFilterValue("max_change", rawValue)?.let { where(table.maxChange eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameNatureEventStatChangesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameNatureEventStatChangesRequest): GameNatureEventStatChangesResponse =
		repository.save(
			GameNatureEventStatChanges {
				natureId = request.natureId ?: invalidValue("nature_id", "nature_id 不能为空")
				eventStatId = request.eventStatId ?: invalidValue("event_stat_id", "event_stat_id 不能为空")
				maxChange = request.maxChange ?: invalidValue("max_change", "max_change 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameNatureEventStatChangesRequest): GameNatureEventStatChangesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameNatureEventStatChanges {
				this.id = id
				natureId = request.natureId ?: invalidValue("nature_id", "nature_id 不能为空")
				eventStatId = request.eventStatId ?: invalidValue("event_stat_id", "event_stat_id 不能为空")
				maxChange = request.maxChange ?: invalidValue("max_change", "max_change 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameNatureEventStatChanges =
		repository.findNullable(id) ?: notFound("id", "性格活动能力变化不存在: $id")

	private fun GameNatureEventStatChanges.toResponse(): GameNatureEventStatChangesResponse =
		GameNatureEventStatChangesResponse {
			id = this@toResponse.id
			natureId = this@toResponse.natureId
			eventStatId = this@toResponse.eventStatId
			maxChange = this@toResponse.maxChange
		}
}
