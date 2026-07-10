package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameItemGameIndicesRequest
import io.github.lishangbu.gamedata.dto.GameItemGameIndicesResponse
import io.github.lishangbu.gamedata.entity.GameItemGameIndices
import io.github.lishangbu.gamedata.entity.gameIndex
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.itemId
import io.github.lishangbu.gamedata.repository.GameItemGameIndicesRepository
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
 * 道具索引维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameItemGameIndicesService(
	private val repository: GameItemGameIndicesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameItemGameIndicesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameItemGameIndices::class) {
			search.pattern?.let { pattern ->
				where(sql<String>("cast(%e as text)", table.itemId) ilike pattern)
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"item_id" -> gameDataLongFilterValue("item_id", rawValue)?.let { where(table.itemId eq it) }
				"game_index" -> gameDataIntFilterValue("game_index", rawValue)?.let { where(table.gameIndex eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameItemGameIndicesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameItemGameIndicesRequest): GameItemGameIndicesResponse =
		repository.save(
			GameItemGameIndices {
				itemId = request.itemId ?: invalidValue("item_id", "item_id 不能为空")
				gameIndex = request.gameIndex ?: invalidValue("game_index", "game_index 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameItemGameIndicesRequest): GameItemGameIndicesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameItemGameIndices {
				this.id = id
				itemId = request.itemId ?: invalidValue("item_id", "item_id 不能为空")
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

	private fun entityByIdOrNotFound(id: Long): GameItemGameIndices =
		repository.findNullable(id) ?: notFound("id", "道具索引不存在: $id")

	private fun GameItemGameIndices.toResponse(): GameItemGameIndicesResponse =
		GameItemGameIndicesResponse {
			id = this@toResponse.id
			itemId = this@toResponse.itemId
			gameIndex = this@toResponse.gameIndex
		}
}
