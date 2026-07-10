package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameItemAttributeBindingsRequest
import io.github.lishangbu.gamedata.dto.GameItemAttributeBindingsResponse
import io.github.lishangbu.gamedata.entity.GameItemAttributeBindings
import io.github.lishangbu.gamedata.entity.attributeId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.itemId
import io.github.lishangbu.gamedata.repository.GameItemAttributeBindingsRepository
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
 * 道具属性绑定维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameItemAttributeBindingsService(
	private val repository: GameItemAttributeBindingsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameItemAttributeBindingsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameItemAttributeBindings::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.itemId) ilike pattern, sql<String>("cast(%e as text)", table.attributeId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"item_id" -> gameDataLongFilterValue("item_id", rawValue)?.let { where(table.itemId eq it) }
				"attribute_id" -> gameDataLongFilterValue("attribute_id", rawValue)?.let { where(table.attributeId eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameItemAttributeBindingsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameItemAttributeBindingsRequest): GameItemAttributeBindingsResponse =
		repository.save(
			GameItemAttributeBindings {
				itemId = request.itemId ?: invalidValue("item_id", "item_id 不能为空")
				attributeId = request.attributeId ?: invalidValue("attribute_id", "attribute_id 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameItemAttributeBindingsRequest): GameItemAttributeBindingsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameItemAttributeBindings {
				this.id = id
				itemId = request.itemId ?: invalidValue("item_id", "item_id 不能为空")
				attributeId = request.attributeId ?: invalidValue("attribute_id", "attribute_id 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameItemAttributeBindings =
		repository.findNullable(id) ?: notFound("id", "道具属性绑定不存在: $id")

	private fun GameItemAttributeBindings.toResponse(): GameItemAttributeBindingsResponse =
		GameItemAttributeBindingsResponse {
			id = this@toResponse.id
			itemId = this@toResponse.itemId
			attributeId = this@toResponse.attributeId
		}
}
