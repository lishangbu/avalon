package io.github.lishangbu.battlesession

/** 表示命令基于过期 Snapshot，不能推进当前会话。 */
class SessionRevisionConflictException(
	val expectedRevision: Long,
	val actualRevision: Long,
) : IllegalStateException("expected session revision $expectedRevision but was $actualRevision")
