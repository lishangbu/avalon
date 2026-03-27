package io.github.lishangbu.avalon.idempotent.exception

/**
 * Raised when a duplicate idempotent request is detected.
 */
class IdempotentConflictException(
    val state: IdempotentConflictState,
    val key: String,
) : RuntimeException(
        when (state) {
            IdempotentConflictState.PROCESSING -> "Idempotent request is already being processed for key '$key'."
            IdempotentConflictState.COMPLETED -> "Idempotent request has already completed for key '$key'."
        },
    )

enum class IdempotentConflictState {
    PROCESSING,
    COMPLETED,
}
