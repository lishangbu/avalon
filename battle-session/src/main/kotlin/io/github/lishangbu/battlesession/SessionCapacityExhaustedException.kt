package io.github.lishangbu.battlesession

import java.time.Duration

/** 表示 Runtime 已达到活跃容量上限，并向适配层提供建议重试时间。 */
class SessionCapacityExhaustedException(
	val retryAfter: Duration,
) : IllegalStateException("Battle Session Runtime active capacity is exhausted") {
	val code: String = "battle-session.capacity-exhausted"
}
