package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameMachinesRequest
import io.github.lishangbu.gamedata.dto.GameMachinesResponse
import io.github.lishangbu.gamedata.entity.GameMachines
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.itemId
import io.github.lishangbu.gamedata.entity.skillId
import io.github.lishangbu.gamedata.repository.GameMachinesRepository
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
 * 机器资料维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameMachinesService(
	private val repository: GameMachinesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameMachinesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameMachines::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.itemId) ilike pattern, sql<String>("cast(%e as text)", table.skillId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"item_id" -> gameDataLongFilterValue("item_id", rawValue)?.let { where(table.itemId eq it) }
				"skill_id" -> gameDataLongFilterValue("skill_id", rawValue)?.let { where(table.skillId eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameMachinesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameMachinesRequest): GameMachinesResponse =
		repository.save(
			GameMachines {
				itemId = request.itemId ?: invalidValue("item_id", "item_id 不能为空")
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameMachinesRequest): GameMachinesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameMachines {
				this.id = id
				itemId = request.itemId ?: invalidValue("item_id", "item_id 不能为空")
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameMachines =
		repository.findNullable(id) ?: notFound("id", "机器资料不存在: $id")

	private fun GameMachines.toResponse(): GameMachinesResponse =
		GameMachinesResponse {
			id = this@toResponse.id
			itemId = this@toResponse.itemId
			skillId = this@toResponse.skillId
		}
}
