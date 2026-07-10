package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameCreatureHeldItemsRequest
import io.github.lishangbu.gamedata.dto.GameCreatureHeldItemsResponse
import io.github.lishangbu.gamedata.entity.GameCreatureHeldItems
import io.github.lishangbu.gamedata.entity.creatureId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.itemId
import io.github.lishangbu.gamedata.entity.rarity
import io.github.lishangbu.gamedata.repository.GameCreatureHeldItemsRepository
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
 * 精灵持有道具维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameCreatureHeldItemsService(
	private val repository: GameCreatureHeldItemsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameCreatureHeldItemsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameCreatureHeldItems::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.creatureId) ilike pattern, sql<String>("cast(%e as text)", table.itemId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"creature_id" -> gameDataLongFilterValue("creature_id", rawValue)?.let { where(table.creatureId eq it) }
				"item_id" -> gameDataLongFilterValue("item_id", rawValue)?.let { where(table.itemId eq it) }
				"rarity" -> gameDataIntFilterValue("rarity", rawValue)?.let { where(table.rarity eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameCreatureHeldItemsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameCreatureHeldItemsRequest): GameCreatureHeldItemsResponse =
		repository.save(
			GameCreatureHeldItems {
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				itemId = request.itemId ?: invalidValue("item_id", "item_id 不能为空")
				rarity = request.rarity
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameCreatureHeldItemsRequest): GameCreatureHeldItemsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameCreatureHeldItems {
				this.id = id
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				itemId = request.itemId ?: invalidValue("item_id", "item_id 不能为空")
				rarity = request.rarity
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameCreatureHeldItems =
		repository.findNullable(id) ?: notFound("id", "精灵持有道具不存在: $id")

	private fun GameCreatureHeldItems.toResponse(): GameCreatureHeldItemsResponse =
		GameCreatureHeldItemsResponse {
			id = this@toResponse.id
			creatureId = this@toResponse.creatureId
			itemId = this@toResponse.itemId
			rarity = this@toResponse.rarity
		}
}
