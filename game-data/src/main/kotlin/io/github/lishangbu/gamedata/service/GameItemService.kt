package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameItemRequest
import io.github.lishangbu.gamedata.dto.GameItemResponse
import io.github.lishangbu.gamedata.catalog.PublishedContentPackService
import io.github.lishangbu.gamedata.entity.GameItem
import io.github.lishangbu.gamedata.entity.ItemUsageType
import io.github.lishangbu.gamedata.entity.categoryId
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.cost
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.flingPower
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.name
import io.github.lishangbu.gamedata.repository.GameItemRepository
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
 * 道具资料维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameItemService(
	private val repository: GameItemRepository,
	private val sqlClient: KSqlClient,
	private val contentPacks: PublishedContentPackService,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameItemResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameItem::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"code" -> gameDataStringFilterValue("code", rawValue)?.let { where(table.code eq it) }
				"name" -> gameDataStringFilterValue("name", rawValue)?.let { where(table.name eq it) }
				"category_id" -> gameDataLongFilterValue("category_id", rawValue)?.let { where(table.categoryId eq it) }
				"cost" -> gameDataIntFilterValue("cost", rawValue)?.let { where(table.cost eq it) }
				"fling_power" -> gameDataIntFilterValue("fling_power", rawValue)?.let { where(table.flingPower eq it) }
				"enabled" -> gameDataBooleanFilterValue("enabled", rawValue)?.let { where(table.enabled eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameItemResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameItemRequest): GameItemResponse {
		val code = request.code.orEmpty().requiredSlugCode("code")
		val contentPackId = contentPacks.requireId()
		return repository.save(
			GameItem {
				this.contentPackId = contentPackId
				this.code = code
				usageType = ItemUsageType.MATERIAL
				iconAssetKey = "content-packs/$contentPackId/items/$code/icon.webp"
				name = gameDataRequiredText(request.name, "name", 120)
				categoryId = request.categoryId
				cost = request.cost
				flingPower = request.flingPower
				enabled = request.enabled
			},
			SaveMode.INSERT_ONLY,
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: GameItemRequest): GameItemResponse {
		val existing = entityByIdOrNotFound(id)
		return repository.save(
			GameItem {
				this.id = id
				contentPackId = existing.contentPackId
				usageType = existing.usageType
				iconAssetKey = existing.iconAssetKey
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				categoryId = request.categoryId
				cost = request.cost
				flingPower = request.flingPower
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

	private fun entityByIdOrNotFound(id: Long): GameItem =
		repository.findNullable(id) ?: notFound("id", "道具资料不存在: $id")

	private fun GameItem.toResponse(): GameItemResponse =
		GameItemResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			categoryId = this@toResponse.categoryId
			cost = this@toResponse.cost
			flingPower = this@toResponse.flingPower
			enabled = this@toResponse.enabled
		}
}
