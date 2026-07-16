package io.github.lishangbu.match.game

/**
 * Match 从创建、激活到完成或中断的纯领域状态机。
 *
 * 每次有效迁移都会推进 [revision]，持久层可据此执行乐观并发控制；终态不可再次迁移。
 */
@ConsistentCopyVisibility
data class MatchLifecycle private constructor(
	val status: MatchStatus,
	val battleSessionId: String?,
	val result: MatchResult?,
	val interruptionReason: MatchInterruptionReason?,
	val revision: Long,
) {
	companion object {
		fun starting() = MatchLifecycle(MatchStatus.STARTING, null, null, null, 0)
		fun preview() = MatchLifecycle(MatchStatus.PREVIEW, null, null, null, 0)
	}

	fun start(): MatchLifecycle {
		require(status == MatchStatus.PREVIEW)
		return copy(status = MatchStatus.STARTING, revision = revision + 1)
	}

	fun activate(sessionId: String): MatchLifecycle {
		require(status == MatchStatus.STARTING)
		return copy(status = MatchStatus.ACTIVE, battleSessionId = sessionId, revision = revision + 1)
	}

	fun complete(result: MatchResult): MatchLifecycle {
		if (status == MatchStatus.COMPLETED || status == MatchStatus.INTERRUPTED) throw MatchAlreadyTerminalException()
		require(status == MatchStatus.ACTIVE)
		return copy(status = MatchStatus.COMPLETED, result = result, revision = revision + 1)
	}

	fun interrupt(reason: MatchInterruptionReason): MatchLifecycle {
		if (status == MatchStatus.COMPLETED || status == MatchStatus.INTERRUPTED) throw MatchAlreadyTerminalException()
		return copy(status = MatchStatus.INTERRUPTED, interruptionReason = reason, revision = revision + 1)
	}
}
