package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameElementGameIndicesRequest
import io.github.lishangbu.gamedata.dto.GameElementGameIndicesResponse
import io.github.lishangbu.gamedata.entity.GameElementGameIndices
import io.github.lishangbu.gamedata.entity.elementId
import io.github.lishangbu.gamedata.entity.gameIndex
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.repository.GameElementGameIndicesRepository
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
 * 属性索引维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameElementGameIndicesService(
	private val repository: GameElementGameIndicesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameElementGameIndicesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameElementGameIndices::class) {
			search.pattern?.let { pattern ->
				where(sql<String>("cast(%e as text)", table.elementId) ilike pattern)
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"element_id" -> gameDataLongFilterValue("element_id", rawValue)?.let { where(table.elementId eq it) }
				"game_index" -> gameDataIntFilterValue("game_index", rawValue)?.let { where(table.gameIndex eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameElementGameIndicesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameElementGameIndicesRequest): GameElementGameIndicesResponse =
		repository.save(
			GameElementGameIndices {
				elementId = request.elementId ?: invalidValue("element_id", "element_id 不能为空")
				gameIndex = request.gameIndex ?: invalidValue("game_index", "game_index 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameElementGameIndicesRequest): GameElementGameIndicesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameElementGameIndices {
				this.id = id
				elementId = request.elementId ?: invalidValue("element_id", "element_id 不能为空")
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

	private fun entityByIdOrNotFound(id: Long): GameElementGameIndices =
		repository.findNullable(id) ?: notFound("id", "属性索引不存在: $id")

	private fun GameElementGameIndices.toResponse(): GameElementGameIndicesResponse =
		GameElementGameIndicesResponse {
			id = this@toResponse.id
			elementId = this@toResponse.elementId
			gameIndex = this@toResponse.gameIndex
		}
}
