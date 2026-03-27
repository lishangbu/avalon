package io.github.lishangbu.avalon.idempotent.aspect

import io.github.lishangbu.avalon.idempotent.annotation.Idempotent
import io.github.lishangbu.avalon.idempotent.exception.IdempotentConflictException
import io.github.lishangbu.avalon.idempotent.exception.IdempotentConflictState
import io.github.lishangbu.avalon.idempotent.key.IdempotentKeyResolver
import io.github.lishangbu.avalon.idempotent.lease.IdempotentLeaseManager
import io.github.lishangbu.avalon.idempotent.properties.IdempotentProperties
import io.github.lishangbu.avalon.idempotent.store.IdempotentStore
import io.github.lishangbu.avalon.idempotent.support.DuplicateStrategy
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.aop.support.AopUtils
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tools.jackson.databind.json.JsonMapper
import java.lang.reflect.Method
import java.util.UUID

/**
 * Intercepts methods annotated with [Idempotent].
 */
@Aspect
class IdempotentAspect(
    private val properties: IdempotentProperties,
    private val keyResolver: IdempotentKeyResolver,
    private val idempotentLeaseManager: IdempotentLeaseManager,
    private val idempotentStore: IdempotentStore,
    private val jsonMapper: JsonMapper,
) {
    @Around("@annotation(annotation)")
    fun around(
        joinPoint: ProceedingJoinPoint,
        annotation: Idempotent,
    ): Any? {
        val method = resolveMethod(joinPoint)
        val storageKey = buildStorageKey(annotation, keyResolver.resolve(joinPoint, method, annotation))
        val token = UUID.randomUUID().toString()

        return when (
            val acquireResult =
                idempotentStore.acquire(
                    key = storageKey,
                    token = token,
                    processingTtl = properties.processingTtl,
                )
        ) {
            is IdempotentStore.AcquireResult.Acquired -> {
                val leaseHandle = idempotentLeaseManager.start(storageKey, token)
                invokeAndFinalize(joinPoint, method, storageKey, token, leaseHandle)
            }

            is IdempotentStore.AcquireResult.Completed -> {
                handleCompletedRequest(
                    annotation = annotation,
                    key = storageKey,
                    method = method,
                    cachedValue = acquireResult.cachedValue,
                )
            }

            is IdempotentStore.AcquireResult.Processing -> {
                throw IdempotentConflictException(
                    state = IdempotentConflictState.PROCESSING,
                    key = storageKey,
                )
            }
        }
    }

    private fun invokeAndFinalize(
        joinPoint: ProceedingJoinPoint,
        method: Method,
        key: String,
        token: String,
        leaseHandle: IdempotentLeaseManager.LeaseHandle,
    ): Any? =
        try {
            val result = joinPoint.proceed()
            val cachedValue = serializeResult(method, result)
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                    object : TransactionSynchronization {
                        override fun afterCommit() {
                            idempotentStore.complete(
                                key = key,
                                token = token,
                                cachedValue = cachedValue,
                                ttl = properties.ttl,
                            )
                        }

                        override fun afterCompletion(status: Int) {
                            leaseHandle.stop()
                            if (status != TransactionSynchronization.STATUS_COMMITTED) {
                                idempotentStore.release(key = key, token = token)
                            }
                        }
                    },
                )
            } else {
                idempotentStore.complete(
                    key = key,
                    token = token,
                    cachedValue = cachedValue,
                    ttl = properties.ttl,
                )
                leaseHandle.stop()
            }
            result
        } catch (ex: Throwable) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                    object : TransactionSynchronization {
                        override fun afterCompletion(status: Int) {
                            leaseHandle.stop()
                            idempotentStore.release(key = key, token = token)
                        }
                    },
                )
            } else {
                idempotentStore.release(key = key, token = token)
                leaseHandle.stop()
            }
            throw ex
        }

    private fun handleCompletedRequest(
        annotation: Idempotent,
        key: String,
        method: Method,
        cachedValue: String?,
    ): Any? =
        when (annotation.duplicateStrategy) {
            DuplicateStrategy.REJECT -> throw IdempotentConflictException(
                state = IdempotentConflictState.COMPLETED,
                key = key,
            )

            DuplicateStrategy.RETURN_CACHED -> deserializeResult(method, cachedValue)
        }

    private fun serializeResult(
        method: Method,
        result: Any?,
    ): String? {
        if (method.returnType == Void.TYPE) {
            return null
        }
        return jsonMapper.writeValueAsString(result)
    }

    private fun deserializeResult(
        method: Method,
        cachedValue: String?,
    ): Any? {
        if (method.returnType == Void.TYPE || cachedValue == null) {
            return null
        }
        return jsonMapper.readValue(cachedValue, jsonMapper.typeFactory.constructType(method.genericReturnType))
    }

    private fun resolveMethod(joinPoint: ProceedingJoinPoint): Method {
        val signature = joinPoint.signature as MethodSignature
        return AopUtils.getMostSpecificMethod(signature.method, joinPoint.target.javaClass)
    }

    private fun buildStorageKey(
        annotation: Idempotent,
        key: String,
    ): String =
        buildString {
            append(properties.keyPrefix)
            if (annotation.prefix.isNotBlank()) {
                append(':')
                append(annotation.prefix)
            }
            append(':')
            append(key)
        }
}
