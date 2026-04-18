package io.github.lishangbu.avalon.app.interfaces.http.common.observability

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import org.jboss.logmanager.MDC
import org.jboss.resteasy.reactive.server.ServerRequestFilter
import org.jboss.resteasy.reactive.server.ServerResponseFilter
import java.util.UUID

/**
 * HTTP 请求关联过滤器。
 *
 * 这里负责为进入 REST 接口的请求补齐 `X-Request-ID`，并把它写入 MDC，
 * 让 access log、控制台业务日志和响应头都能共享同一个请求标识。
 * 当前只处理 Quarkus REST 路径；`/q/metrics`、`/q/health` 这类非应用端点
 * 仍由框架默认链路处理。
 */
@ApplicationScoped
class RequestCorrelationFilters {
    /**
     * 在请求进入资源方法前补齐请求标识并写入 MDC。
     *
     * 如果上游网关已经传入 `X-Request-ID`，则直接复用；否则由服务端生成新的 UUID。
     * 这样既能兼容网关透传，也能保证本地直连调试时每个请求都有稳定关联键。
     *
     * @param requestContext 当前 HTTP 请求上下文。
     */
    @ServerRequestFilter(preMatching = true)
    fun attachRequestId(requestContext: ContainerRequestContext) {
        val requestId = requestContext.getHeaderString(REQUEST_ID_HEADER)?.trim()?.takeIf { it.isNotEmpty() } ?: UUID.randomUUID().toString()
        requestContext.headers.putSingle(REQUEST_ID_HEADER, requestId)
        requestContext.setProperty(REQUEST_ID_PROPERTY, requestId)
        MDC.put(REQUEST_ID_MDC_KEY, requestId)
    }

    /**
     * 在响应返回前回写请求标识并清理 MDC。
     *
     * 这样客户端总能拿到最终使用的 `X-Request-ID`，同时避免线程复用时把上一个请求
     * 的 MDC 信息泄漏到后续日志里。
     *
     * @param requestContext 当前 HTTP 请求上下文。
     * @param responseContext 当前 HTTP 响应上下文。
     */
    @ServerResponseFilter
    fun detachRequestId(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        val requestId = requestContext.getProperty(REQUEST_ID_PROPERTY) as? String ?: requestContext.getHeaderString(REQUEST_ID_HEADER)
        if (!requestId.isNullOrBlank()) {
            responseContext.headers.putSingle(REQUEST_ID_HEADER, requestId)
        }
        MDC.remove(REQUEST_ID_MDC_KEY)
    }

    private companion object {
        private const val REQUEST_ID_HEADER = "X-Request-ID"
        private const val REQUEST_ID_PROPERTY = "avalon.request.id"
        private const val REQUEST_ID_MDC_KEY = "request.id"
    }
}
