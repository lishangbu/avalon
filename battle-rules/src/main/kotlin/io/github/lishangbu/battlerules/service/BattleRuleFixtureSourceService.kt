package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleRuleFixtureSourceRequest
import io.github.lishangbu.battlerules.dto.BattleRuleFixtureSourceResponse
import io.github.lishangbu.battlerules.entity.BattleRuleFixtureSource
import io.github.lishangbu.battlerules.entity.fixtureId
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.sourceLabel
import io.github.lishangbu.battlerules.entity.sourceNote
import io.github.lishangbu.battlerules.entity.sourceUrl
import io.github.lishangbu.battlerules.repository.BattleRuleFixtureSourceRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredText
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗规则 Fixture 公开来源维护服务。
 *
 * 来源链接是 Fixture 的从属资料，创建和更新时必须确认 Fixture 存在。服务层只允许 HTTPS 链接，
 * 这是为了让管理端展示的公开来源可以安全地被浏览器打开，并避免把本地文件路径写进业务资料。
 */
@Service
class BattleRuleFixtureSourceService(
	private val repository: BattleRuleFixtureSourceRepository,
	private val fixtureService: BattleRuleFixtureService,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, fixtureId: Long?): Page<BattleRuleFixtureSourceResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleRuleFixtureSource::class) {
			fixtureId?.let { where(table.fixtureId eq requiredPositiveId(it, "fixtureId")) }
			orderBy(table.fixtureId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleRuleFixtureSourceResponse =
		sourceByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleRuleFixtureSourceRequest): BattleRuleFixtureSourceResponse {
		val normalized = request.normalized()
		fixtureService.fixtureByIdOrNotFound(normalized.fixtureId)
		ensureSourceAvailable(normalized.fixtureId, normalized.sourceUrl, null)
		return repository.save(
			BattleRuleFixtureSource {
				fixtureId = normalized.fixtureId
				sourceUrl = normalized.sourceUrl
				sourceLabel = normalized.sourceLabel
				sourceNote = normalized.sourceNote
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: BattleRuleFixtureSourceRequest): BattleRuleFixtureSourceResponse {
		sourceByIdOrNotFound(id)
		val normalized = request.normalized()
		fixtureService.fixtureByIdOrNotFound(normalized.fixtureId)
		ensureSourceAvailable(normalized.fixtureId, normalized.sourceUrl, id)
		return repository.save(
			BattleRuleFixtureSource {
				this.id = id
				fixtureId = normalized.fixtureId
				sourceUrl = normalized.sourceUrl
				sourceLabel = normalized.sourceLabel
				sourceNote = normalized.sourceNote
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		sourceByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun sourceByIdOrNotFound(id: Long): BattleRuleFixtureSource =
		repository.findNullable(id) ?: notFound("id", "Fixture 公开来源不存在: $id")

	private fun ensureSourceAvailable(fixtureId: Long, sourceUrl: String, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleRuleFixtureSource::class) {
			where(table.fixtureId eq fixtureId)
			where(table.sourceUrl eq sourceUrl)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("sourceUrl", "该 Fixture 已经维护相同来源链接")
		}
	}

	private fun BattleRuleFixtureSourceRequest.normalized(): BattleRuleFixtureSourceRequest =
		copy(
			fixtureId = requiredPositiveId(fixtureId, "fixtureId"),
			sourceUrl = sourceUrl.requiredHttpsUrl("sourceUrl", 600),
			sourceLabel = optionalText(sourceLabel, "sourceLabel", 160),
			sourceNote = optionalText(sourceNote, "sourceNote", 600),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun String.requiredHttpsUrl(fieldName: String, maxLength: Int): String {
		val text = requiredText(fieldName, maxLength)
		if (!text.startsWith("https://")) {
			invalidValue(fieldName, "$fieldName 必须使用 https://")
		}
		return text
	}

	private fun BattleRuleFixtureSource.toResponse(): BattleRuleFixtureSourceResponse =
		BattleRuleFixtureSourceResponse(
			id = id,
			fixtureId = fixtureId,
			sourceUrl = sourceUrl,
			sourceLabel = sourceLabel,
			sourceNote = sourceNote,
			sortOrder = sortOrder,
		)
}
