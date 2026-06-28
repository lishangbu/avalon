package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleRuleTestRunRequest
import io.github.lishangbu.battlerules.dto.BattleRuleTestRunResponse
import io.github.lishangbu.battlerules.entity.BattleRuleTestRun
import io.github.lishangbu.battlerules.entity.assertionCount
import io.github.lishangbu.battlerules.entity.command
import io.github.lishangbu.battlerules.entity.durationMs
import io.github.lishangbu.battlerules.entity.engineCommit
import io.github.lishangbu.battlerules.entity.executor
import io.github.lishangbu.battlerules.entity.failureMessage
import io.github.lishangbu.battlerules.entity.finishedAt
import io.github.lishangbu.battlerules.entity.fixtureId
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.runCode
import io.github.lishangbu.battlerules.entity.runStatus
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.startedAt
import io.github.lishangbu.battlerules.repository.BattleRuleTestRunRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.requiredText
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 战斗规则 Fixture 测试运行结果维护服务。
 *
 * 该服务不直接执行 Gradle 或引擎测试，只保存一次运行的结构化结论。真正执行可以来自本地、CI 或后续专门
 * 的测试执行器；这里负责保证运行结果能被管理端检索、筛选和审阅。
 */
@Service
class BattleRuleTestRunService(
	private val repository: BattleRuleTestRunRepository,
	private val fixtureService: BattleRuleFixtureService,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, fixtureId: Long?, runStatus: String?): Page<BattleRuleTestRunResponse> {
		validatePage(page, size)
		val normalizedStatus = runStatus?.takeIf { it.isNotBlank() }?.let { requiredRunStatus(it) }
		return sqlClient.createQuery(BattleRuleTestRun::class) {
			fixtureId?.let { where(table.fixtureId eq requiredPositiveId(it, "fixtureId")) }
			normalizedStatus?.let { where(table.runStatus eq it) }
			orderBy(table.startedAt.desc(), table.id.desc())
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleRuleTestRunResponse =
		runByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleRuleTestRunRequest): BattleRuleTestRunResponse {
		val runCode = request.runCode.requiredSlugCode("runCode")
		ensureRunCodeAvailable(runCode, null)
		val normalized = request.normalized()
		fixtureService.fixtureByIdOrNotFound(normalized.fixtureId)
		return repository.save(
			BattleRuleTestRun {
				this.runCode = runCode
				fixtureId = normalized.fixtureId
				runStatus = normalized.runStatus
				executor = normalized.executor
				command = normalized.command
				engineCommit = normalized.engineCommit
				startedAt = normalized.startedAt ?: OffsetDateTime.now(ZoneOffset.UTC)
				finishedAt = normalized.finishedAt
				durationMs = normalized.durationMs
				assertionCount = normalized.assertionCount
				failureMessage = normalized.failureMessage
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: BattleRuleTestRunRequest): BattleRuleTestRunResponse {
		runByIdOrNotFound(id)
		val runCode = request.runCode.requiredSlugCode("runCode")
		ensureRunCodeAvailable(runCode, id)
		val normalized = request.normalized()
		fixtureService.fixtureByIdOrNotFound(normalized.fixtureId)
		return repository.save(
			BattleRuleTestRun {
				this.id = id
				this.runCode = runCode
				fixtureId = normalized.fixtureId
				runStatus = normalized.runStatus
				executor = normalized.executor
				command = normalized.command
				engineCommit = normalized.engineCommit
				startedAt = normalized.startedAt ?: OffsetDateTime.now(ZoneOffset.UTC)
				finishedAt = normalized.finishedAt
				durationMs = normalized.durationMs
				assertionCount = normalized.assertionCount
				failureMessage = normalized.failureMessage
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		runByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun runByIdOrNotFound(id: Long): BattleRuleTestRun =
		repository.findNullable(id) ?: notFound("id", "Fixture 测试运行结果不存在: $id")

	private fun ensureRunCodeAvailable(runCode: String, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleRuleTestRun::class) {
			where(table.runCode eq runCode)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("runCode", "测试运行 code 已存在: $runCode")
		}
	}

	private fun BattleRuleTestRunRequest.normalized(): BattleRuleTestRunRequest =
		copy(
			fixtureId = requiredPositiveId(fixtureId, "fixtureId"),
			runStatus = requiredRunStatus(runStatus),
			executor = executor.requiredText("executor", 80),
			command = optionalText(command, "command", 300),
			engineCommit = optionalText(engineCommit, "engineCommit", 80),
			durationMs = optionalLongRange(durationMs, "durationMs", 0, 86_400_000),
			assertionCount = optionalIntRange(assertionCount, "assertionCount", 0, 1_000_000),
			failureMessage = optionalText(failureMessage, "failureMessage", 1000),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun requiredRunStatus(value: String): String {
		val status = requiredUpperText(value, "runStatus", 30)
		if (status !in RUN_STATUSES) {
			invalidValue("runStatus", "runStatus 只能是 ${RUN_STATUSES.joinToString()}")
		}
		return status
	}

	private fun optionalLongRange(value: Long?, fieldName: String, min: Long, max: Long): Long? {
		if (value == null) {
			return null
		}
		if (value !in min..max) {
			invalidValue(fieldName, "$fieldName 必须在 $min 到 $max 之间")
		}
		return value
	}

	private fun BattleRuleTestRun.toResponse(): BattleRuleTestRunResponse =
		BattleRuleTestRunResponse(
			id = id,
			runCode = runCode,
			fixtureId = fixtureId,
			runStatus = runStatus,
			executor = executor,
			command = command,
			engineCommit = engineCommit,
			startedAt = startedAt,
			finishedAt = finishedAt,
			durationMs = durationMs,
			assertionCount = assertionCount,
			failureMessage = failureMessage,
			sortOrder = sortOrder,
		)

	private companion object {
		private val RUN_STATUSES = setOf("PASSED", "FAILED", "SKIPPED", "RUNNING")
	}
}
