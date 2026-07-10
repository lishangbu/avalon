package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameCreatureElementRequest
import io.github.lishangbu.gamedata.dto.GameCreatureElementResponse
import io.github.lishangbu.gamedata.entity.GameCreatureElement
import io.github.lishangbu.gamedata.entity.creatureId
import io.github.lishangbu.gamedata.entity.elementId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.slotOrder
import io.github.lishangbu.gamedata.repository.GameCreatureElementRepository
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
 * 精灵属性绑定维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameCreatureElementService(
	private val repository: GameCreatureElementRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameCreatureElementResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameCreatureElement::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.creatureId) ilike pattern, sql<String>("cast(%e as text)", table.elementId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"creature_id" -> gameDataLongFilterValue("creature_id", rawValue)?.let { where(table.creatureId eq it) }
				"element_id" -> gameDataLongFilterValue("element_id", rawValue)?.let { where(table.elementId eq it) }
				"slot_order" -> gameDataIntFilterValue("slot_order", rawValue)?.let { where(table.slotOrder eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameCreatureElementResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameCreatureElementRequest): GameCreatureElementResponse =
		repository.save(
			GameCreatureElement {
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				elementId = request.elementId ?: invalidValue("element_id", "element_id 不能为空")
				slotOrder = request.slotOrder ?: invalidValue("slot_order", "slot_order 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameCreatureElementRequest): GameCreatureElementResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameCreatureElement {
				this.id = id
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				elementId = request.elementId ?: invalidValue("element_id", "element_id 不能为空")
				slotOrder = request.slotOrder ?: invalidValue("slot_order", "slot_order 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameCreatureElement =
		repository.findNullable(id) ?: notFound("id", "精灵属性绑定不存在: $id")

	private fun GameCreatureElement.toResponse(): GameCreatureElementResponse =
		GameCreatureElementResponse {
			id = this@toResponse.id
			creatureId = this@toResponse.creatureId
			elementId = this@toResponse.elementId
			slotOrder = this@toResponse.slotOrder
		}
}
