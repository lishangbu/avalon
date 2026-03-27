package io.github.lishangbu.avalon.idempotent.support

/**
 * Strategy used when a completed idempotent request is repeated.
 */
enum class DuplicateStrategy {
    REJECT,
    RETURN_CACHED,
}
