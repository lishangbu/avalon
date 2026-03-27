package io.github.lishangbu.avalon.idempotent.key

import io.github.lishangbu.avalon.idempotent.annotation.Idempotent
import org.aspectj.lang.ProceedingJoinPoint
import java.lang.reflect.Method

/**
 * Resolves the final idempotent key for an invocation.
 */
interface IdempotentKeyResolver {
    fun resolve(
        joinPoint: ProceedingJoinPoint,
        method: Method,
        annotation: Idempotent,
    ): String
}
