package io.github.lishangbu.match.trainer

import java.time.Instant

/** 建立或验证 Trainer Session 后返回的 opaque credential 与当前 Trainer。 */
data class TrainerSessionResponse(val credential: String, val expiresAt: Instant, val trainer: TrainerResponse) {
	companion object {
		fun from(view: TrainerSessionView) = TrainerSessionResponse(
			view.session.credential,
			view.session.expiresAt,
			view.trainer.toResponse(),
		)
	}
}
