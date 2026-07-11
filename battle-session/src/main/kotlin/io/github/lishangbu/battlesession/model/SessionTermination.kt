package io.github.lishangbu.battlesession.model

import java.time.Instant

/** 记录显式终止命令提交后的 revision 变化、原因与服务端时间。 */
data class SessionTermination(
	val commandId: String,
	val reason: String,
	val revisionBefore: Long,
	val revisionAfter: Long,
	val terminatedAt: Instant,
)
