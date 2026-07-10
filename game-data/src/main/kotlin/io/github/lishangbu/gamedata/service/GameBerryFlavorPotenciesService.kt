package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameBerryFlavorPotenciesRequest
import io.github.lishangbu.gamedata.dto.GameBerryFlavorPotenciesResponse
import io.github.lishangbu.gamedata.entity.GameBerryFlavorPotencies
import io.github.lishangbu.gamedata.entity.berryId
import io.github.lishangbu.gamedata.entity.flavorId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.potency
import io.github.lishangbu.gamedata.repository.GameBerryFlavorPotenciesRepository
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
 * 树果口味强度维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameBerryFlavorPotenciesService(
	private val repository: GameBerryFlavorPotenciesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameBerryFlavorPotenciesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameBerryFlavorPotencies::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.berryId) ilike pattern, sql<String>("cast(%e as text)", table.flavorId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"berry_id" -> gameDataLongFilterValue("berry_id", rawValue)?.let { where(table.berryId eq it) }
				"flavor_id" -> gameDataLongFilterValue("flavor_id", rawValue)?.let { where(table.flavorId eq it) }
				"potency" -> gameDataIntFilterValue("potency", rawValue)?.let { where(table.potency eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameBerryFlavorPotenciesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameBerryFlavorPotenciesRequest): GameBerryFlavorPotenciesResponse =
		repository.save(
			GameBerryFlavorPotencies {
				berryId = request.berryId ?: invalidValue("berry_id", "berry_id 不能为空")
				flavorId = request.flavorId ?: invalidValue("flavor_id", "flavor_id 不能为空")
				potency = request.potency ?: invalidValue("potency", "potency 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameBerryFlavorPotenciesRequest): GameBerryFlavorPotenciesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameBerryFlavorPotencies {
				this.id = id
				berryId = request.berryId ?: invalidValue("berry_id", "berry_id 不能为空")
				flavorId = request.flavorId ?: invalidValue("flavor_id", "flavor_id 不能为空")
				potency = request.potency ?: invalidValue("potency", "potency 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameBerryFlavorPotencies =
		repository.findNullable(id) ?: notFound("id", "树果口味强度不存在: $id")

	private fun GameBerryFlavorPotencies.toResponse(): GameBerryFlavorPotenciesResponse =
		GameBerryFlavorPotenciesResponse {
			id = this@toResponse.id
			berryId = this@toResponse.berryId
			flavorId = this@toResponse.flavorId
			potency = this@toResponse.potency
		}
}
