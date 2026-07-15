package io.github.lishangbu.match.trainer

import java.time.Instant

/** 仅驻留单节点内存、带滑动空闲期限的 Trainer 身份凭据。 */
data class TrainerSession(
	val accountId: Long,
	val trainerId: Long,
	val credential: String,
	val expiresAt: Instant,
	val loginToken: String? = null,
)
