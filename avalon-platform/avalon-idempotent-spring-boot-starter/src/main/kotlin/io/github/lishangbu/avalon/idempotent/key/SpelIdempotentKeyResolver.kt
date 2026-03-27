package io.github.lishangbu.avalon.idempotent.key

import io.github.lishangbu.avalon.idempotent.annotation.Idempotent
import io.github.lishangbu.avalon.idempotent.properties.IdempotentProperties
import org.aspectj.lang.ProceedingJoinPoint
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.lang.reflect.Method

/**
 * Resolves idempotent keys from SpEL expressions.
 */
class SpelIdempotentKeyResolver(
    private val properties: IdempotentProperties,
) : IdempotentKeyResolver {
    private val parser = SpelExpressionParser()
    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    override fun resolve(
        joinPoint: ProceedingJoinPoint,
        method: Method,
        annotation: Idempotent,
    ): String {
        if (annotation.key.isBlank()) {
            return resolveFromRequestHeader(method)
        }

        val context =
            MethodBasedEvaluationContext(
                joinPoint.target,
                method,
                joinPoint.args,
                parameterNameDiscoverer,
            ).apply {
                setVariable("method", method)
                setVariable("target", joinPoint.target)
            }
        val value = parser.parseExpression(annotation.key).getValue(context as StandardEvaluationContext)
        return value?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: error("Resolved idempotent key is blank for method ${method.declaringClass.name}.${method.name}.")
    }

    private fun resolveFromRequestHeader(method: Method): String {
        val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val headerValue =
            requestAttributes
                ?.request
                ?.getHeader(properties.headerName)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        return headerValue
            ?: error(
                "No idempotent key expression was configured for ${method.declaringClass.name}.${method.name}, and request header '${properties.headerName}' is missing.",
            )
    }
}
