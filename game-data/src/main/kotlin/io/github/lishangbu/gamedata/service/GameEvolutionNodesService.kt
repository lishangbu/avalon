package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameEvolutionNodesRequest
import io.github.lishangbu.gamedata.dto.GameEvolutionNodesResponse
import io.github.lishangbu.gamedata.entity.GameEvolutionNodes
import io.github.lishangbu.gamedata.entity.baby
import io.github.lishangbu.gamedata.entity.chainId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.nodeOrder
import io.github.lishangbu.gamedata.entity.parentSpeciesId
import io.github.lishangbu.gamedata.entity.speciesId
import io.github.lishangbu.gamedata.repository.GameEvolutionNodesRepository
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
 * 进化链节点维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameEvolutionNodesService(
	private val repository: GameEvolutionNodesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameEvolutionNodesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameEvolutionNodes::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.chainId) ilike pattern, sql<String>("cast(%e as text)", table.speciesId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"chain_id" -> gameDataLongFilterValue("chain_id", rawValue)?.let { where(table.chainId eq it) }
				"species_id" -> gameDataLongFilterValue("species_id", rawValue)?.let { where(table.speciesId eq it) }
				"parent_species_id" -> gameDataLongFilterValue("parent_species_id", rawValue)?.let { where(table.parentSpeciesId eq it) }
				"baby" -> gameDataBooleanFilterValue("baby", rawValue)?.let { where(table.baby eq it) }
				"node_order" -> gameDataIntFilterValue("node_order", rawValue)?.let { where(table.nodeOrder eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameEvolutionNodesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameEvolutionNodesRequest): GameEvolutionNodesResponse =
		repository.save(
			GameEvolutionNodes {
				chainId = request.chainId ?: invalidValue("chain_id", "chain_id 不能为空")
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				parentSpeciesId = request.parentSpeciesId
				baby = request.baby ?: invalidValue("baby", "baby 不能为空")
				nodeOrder = request.nodeOrder ?: invalidValue("node_order", "node_order 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameEvolutionNodesRequest): GameEvolutionNodesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameEvolutionNodes {
				this.id = id
				chainId = request.chainId ?: invalidValue("chain_id", "chain_id 不能为空")
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				parentSpeciesId = request.parentSpeciesId
				baby = request.baby ?: invalidValue("baby", "baby 不能为空")
				nodeOrder = request.nodeOrder ?: invalidValue("node_order", "node_order 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameEvolutionNodes =
		repository.findNullable(id) ?: notFound("id", "进化链节点不存在: $id")

	private fun GameEvolutionNodes.toResponse(): GameEvolutionNodesResponse =
		GameEvolutionNodesResponse {
			id = this@toResponse.id
			chainId = this@toResponse.chainId
			speciesId = this@toResponse.speciesId
			parentSpeciesId = this@toResponse.parentSpeciesId
			baby = this@toResponse.baby
			nodeOrder = this@toResponse.nodeOrder
		}
}
