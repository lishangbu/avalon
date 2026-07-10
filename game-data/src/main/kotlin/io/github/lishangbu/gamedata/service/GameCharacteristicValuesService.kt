package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameCharacteristicValuesRequest
import io.github.lishangbu.gamedata.dto.GameCharacteristicValuesResponse
import io.github.lishangbu.gamedata.entity.GameCharacteristicValues
import io.github.lishangbu.gamedata.entity.characteristicId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.possibleValue
import io.github.lishangbu.gamedata.repository.GameCharacteristicValuesRepository
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
 * 个体特征取值维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameCharacteristicValuesService(
	private val repository: GameCharacteristicValuesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameCharacteristicValuesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameCharacteristicValues::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.characteristicId) ilike pattern, sql<String>("cast(%e as text)", table.possibleValue) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"characteristic_id" -> gameDataLongFilterValue("characteristic_id", rawValue)?.let { where(table.characteristicId eq it) }
				"possible_value" -> gameDataIntFilterValue("possible_value", rawValue)?.let { where(table.possibleValue eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameCharacteristicValuesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameCharacteristicValuesRequest): GameCharacteristicValuesResponse =
		repository.save(
			GameCharacteristicValues {
				characteristicId = request.characteristicId ?: invalidValue("characteristic_id", "characteristic_id 不能为空")
				possibleValue = request.possibleValue ?: invalidValue("possible_value", "possible_value 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameCharacteristicValuesRequest): GameCharacteristicValuesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameCharacteristicValues {
				this.id = id
				characteristicId = request.characteristicId ?: invalidValue("characteristic_id", "characteristic_id 不能为空")
				possibleValue = request.possibleValue ?: invalidValue("possible_value", "possible_value 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameCharacteristicValues =
		repository.findNullable(id) ?: notFound("id", "个体特征取值不存在: $id")

	private fun GameCharacteristicValues.toResponse(): GameCharacteristicValuesResponse =
		GameCharacteristicValuesResponse {
			id = this@toResponse.id
			characteristicId = this@toResponse.characteristicId
			possibleValue = this@toResponse.possibleValue
		}
}
