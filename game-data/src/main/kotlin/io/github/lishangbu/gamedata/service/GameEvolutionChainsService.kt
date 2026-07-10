package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameEvolutionChainsRequest
import io.github.lishangbu.gamedata.dto.GameEvolutionChainsResponse
import io.github.lishangbu.gamedata.entity.GameEvolutionChains
import io.github.lishangbu.gamedata.entity.babyTriggerItemId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.repository.GameEvolutionChainsRepository
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
 * 进化链维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameEvolutionChainsService(
	private val repository: GameEvolutionChainsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameEvolutionChainsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameEvolutionChains::class) {
			search.pattern?.let { pattern ->
				where(sql<String>("cast(%e as text)", table.babyTriggerItemId) ilike pattern)
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"baby_trigger_item_id" -> gameDataLongFilterValue("baby_trigger_item_id", rawValue)?.let { where(table.babyTriggerItemId eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameEvolutionChainsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameEvolutionChainsRequest): GameEvolutionChainsResponse =
		repository.save(
			GameEvolutionChains {
				babyTriggerItemId = request.babyTriggerItemId
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameEvolutionChainsRequest): GameEvolutionChainsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameEvolutionChains {
				this.id = id
				babyTriggerItemId = request.babyTriggerItemId
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameEvolutionChains =
		repository.findNullable(id) ?: notFound("id", "进化链不存在: $id")

	private fun GameEvolutionChains.toResponse(): GameEvolutionChainsResponse =
		GameEvolutionChainsResponse {
			id = this@toResponse.id
			babyTriggerItemId = this@toResponse.babyTriggerItemId
		}
}
