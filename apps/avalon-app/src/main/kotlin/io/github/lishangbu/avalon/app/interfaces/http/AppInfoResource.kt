package io.github.lishangbu.avalon.app.interfaces.http

import io.github.lishangbu.avalon.shared.application.time.ClockProvider
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces

/**
 * 应用基础信息接口。
 *
 * 当前用于暴露运行中应用的最小自描述信息，方便健康检查之外的
 * 调试、联调和环境确认场景快速识别当前实例的架构基线。
 */
@Path("/api/app-info")
class AppInfoResource(
    private val clockProvider: ClockProvider,
) {
    /**
     * 返回应用的静态元信息和当前生成时间。
     *
     * @return 应用名、架构基线、持久化基线与上下文列表。
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun info(): AppInfoResponse = clockProvider.currentInstant().toAppInfoResponse()
}