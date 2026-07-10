package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameElementRequest
import io.github.lishangbu.gamedata.dto.GameElementResponse
import io.github.lishangbu.gamedata.entity.GameElement
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.name
import io.github.lishangbu.gamedata.entity.sortOrder
import io.github.lishangbu.gamedata.repository.GameElementRepository
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 属性资料维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameElementService(
	private val repository: GameElementRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameElementResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameElement::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"code" -> gameDataStringFilterValue("code", rawValue)?.let { where(table.code eq it) }
				"name" -> gameDataStringFilterValue("name", rawValue)?.let { where(table.name eq it) }
				"sort_order" -> gameDataIntFilterValue("sort_order", rawValue)?.let { where(table.sortOrder eq it) }
				"enabled" -> gameDataBooleanFilterValue("enabled", rawValue)?.let { where(table.enabled eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameElementResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameElementRequest): GameElementResponse =
		repository.save(
			GameElement {
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 80)
				sortOrder = request.sortOrder ?: invalidValue("sort_order", "sort_order 不能为空")
				enabled = request.enabled
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameElementRequest): GameElementResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameElement {
				this.id = id
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 80)
				sortOrder = request.sortOrder ?: invalidValue("sort_order", "sort_order 不能为空")
				enabled = request.enabled
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameElement =
		repository.findNullable(id) ?: notFound("id", "属性资料不存在: $id")

	private fun GameElement.toResponse(): GameElementResponse =
		GameElementResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			sortOrder = this@toResponse.sortOrder
			enabled = this@toResponse.enabled
		}
}
