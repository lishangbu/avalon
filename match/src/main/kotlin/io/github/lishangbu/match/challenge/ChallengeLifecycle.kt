package io.github.lishangbu.match.challenge

import java.time.Instant

/**
 * Challenge 的不可变生命周期值对象。
 *
 * 只有 `PENDING` 可以进入终态；每次成功转换都会递增 revision 并记录服务端解决时间，
 * 从而让 REST 命令和数据库条件更新共享同一套并发语义。
 */
@ConsistentCopyVisibility
data class ChallengeLifecycle private constructor(
	val status: ChallengeStatus,
	val cancellationReason: ChallengeCancellationReason?,
	val revision: Long,
	val createdAt: Instant,
	val resolvedAt: Instant?,
) {
	companion object {
		fun pending(createdAt: Instant) = ChallengeLifecycle(ChallengeStatus.PENDING, null, 0, createdAt, null)
	}

	fun resolve(
		target: ChallengeStatus,
		at: Instant,
		cancellationReason: ChallengeCancellationReason? = null,
	): ChallengeLifecycle {
		if (status != ChallengeStatus.PENDING) throw ChallengeAlreadyResolvedException()
		if (target == ChallengeStatus.PENDING) throw InvalidChallengeTransitionException()
		if ((target == ChallengeStatus.CANCELLED) != (cancellationReason != null)) {
			throw InvalidChallengeTransitionException()
		}
		return copy(status = target, cancellationReason = cancellationReason, revision = revision + 1, resolvedAt = at)
	}
}
