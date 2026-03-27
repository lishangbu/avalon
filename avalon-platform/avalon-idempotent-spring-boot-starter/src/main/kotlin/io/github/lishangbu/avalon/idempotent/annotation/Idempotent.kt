package io.github.lishangbu.avalon.idempotent.annotation

import io.github.lishangbu.avalon.idempotent.support.DuplicateStrategy

/**
 * Marks a method as idempotent.
 *
 * The key must be a SpEL expression resolved against the method arguments.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Idempotent(
    val key: String = "",
    val prefix: String = "",
    val duplicateStrategy: DuplicateStrategy = DuplicateStrategy.REJECT,
)
