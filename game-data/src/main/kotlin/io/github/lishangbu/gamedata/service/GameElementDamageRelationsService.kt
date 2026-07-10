package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameElementDamageRelationsRequest
import io.github.lishangbu.gamedata.dto.GameElementDamageRelationsResponse
import io.github.lishangbu.gamedata.entity.GameElementDamageRelations
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.relationType
import io.github.lishangbu.gamedata.entity.sourceElementId
import io.github.lishangbu.gamedata.entity.targetElementId
import io.github.lishangbu.gamedata.repository.GameElementDamageRelationsRepository
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
 * 属性克制关系维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameElementDamageRelationsService(
	private val repository: GameElementDamageRelationsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameElementDamageRelationsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameElementDamageRelations::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.sourceElementId) ilike pattern, sql<String>("cast(%e as text)", table.targetElementId) ilike pattern, table.relationType ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"source_element_id" -> gameDataLongFilterValue("source_element_id", rawValue)?.let { where(table.sourceElementId eq it) }
				"target_element_id" -> gameDataLongFilterValue("target_element_id", rawValue)?.let { where(table.targetElementId eq it) }
				"relation_type" -> gameDataStringFilterValue("relation_type", rawValue)?.let { where(table.relationType eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameElementDamageRelationsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameElementDamageRelationsRequest): GameElementDamageRelationsResponse =
		repository.save(
			GameElementDamageRelations {
				sourceElementId = request.sourceElementId ?: invalidValue("source_element_id", "source_element_id 不能为空")
				targetElementId = request.targetElementId ?: invalidValue("target_element_id", "target_element_id 不能为空")
				relationType = gameDataRequiredText(request.relationType, "relation_type", 40)
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameElementDamageRelationsRequest): GameElementDamageRelationsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameElementDamageRelations {
				this.id = id
				sourceElementId = request.sourceElementId ?: invalidValue("source_element_id", "source_element_id 不能为空")
				targetElementId = request.targetElementId ?: invalidValue("target_element_id", "target_element_id 不能为空")
				relationType = gameDataRequiredText(request.relationType, "relation_type", 40)
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameElementDamageRelations =
		repository.findNullable(id) ?: notFound("id", "属性克制关系不存在: $id")

	private fun GameElementDamageRelations.toResponse(): GameElementDamageRelationsResponse =
		GameElementDamageRelationsResponse {
			id = this@toResponse.id
			sourceElementId = this@toResponse.sourceElementId
			targetElementId = this@toResponse.targetElementId
			relationType = this@toResponse.relationType
		}
}
