package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameItemCategoryPocketsRequest
import io.github.lishangbu.gamedata.dto.GameItemCategoryPocketsResponse
import io.github.lishangbu.gamedata.entity.GameItemCategoryPockets
import io.github.lishangbu.gamedata.entity.categoryId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.pocketId
import io.github.lishangbu.gamedata.repository.GameItemCategoryPocketsRepository
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
 * 道具分类口袋维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameItemCategoryPocketsService(
	private val repository: GameItemCategoryPocketsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameItemCategoryPocketsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameItemCategoryPockets::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.categoryId) ilike pattern, sql<String>("cast(%e as text)", table.pocketId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"category_id" -> gameDataLongFilterValue("category_id", rawValue)?.let { where(table.categoryId eq it) }
				"pocket_id" -> gameDataLongFilterValue("pocket_id", rawValue)?.let { where(table.pocketId eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameItemCategoryPocketsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameItemCategoryPocketsRequest): GameItemCategoryPocketsResponse =
		repository.save(
			GameItemCategoryPockets {
				categoryId = request.categoryId ?: invalidValue("category_id", "category_id 不能为空")
				pocketId = request.pocketId ?: invalidValue("pocket_id", "pocket_id 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameItemCategoryPocketsRequest): GameItemCategoryPocketsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameItemCategoryPockets {
				this.id = id
				categoryId = request.categoryId ?: invalidValue("category_id", "category_id 不能为空")
				pocketId = request.pocketId ?: invalidValue("pocket_id", "pocket_id 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameItemCategoryPockets =
		repository.findNullable(id) ?: notFound("id", "道具分类口袋不存在: $id")

	private fun GameItemCategoryPockets.toResponse(): GameItemCategoryPocketsResponse =
		GameItemCategoryPocketsResponse {
			id = this@toResponse.id
			categoryId = this@toResponse.categoryId
			pocketId = this@toResponse.pocketId
		}
}
