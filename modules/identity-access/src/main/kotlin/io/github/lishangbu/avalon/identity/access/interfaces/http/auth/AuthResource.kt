package io.github.lishangbu.avalon.identity.access.interfaces.http.auth

import io.github.lishangbu.avalon.identity.access.application.authentication.AuthenticationService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

/**
 * 本地认证相关的 HTTP 入口。
 *
 * 这里对外暴露登录、刷新令牌、会话管理和当前用户查询能力，
 * Resource 本身只负责接收 HTTP 参数并调用贴近 DTO 的映射函数。
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
class AuthResource(
    private val authenticationService: AuthenticationService,
    private val jwt: JsonWebToken,
) {
    /**
     * 执行登录并返回一对令牌。
     *
     * @param request 登录请求体。
     * @param userAgent 当前请求的 `User-Agent`。
     * @param forwardedFor 代理链路透传的客户端 IP。
     * @return 登录成功后的 access token 与 refresh token。
     */
    @POST
    @Path("/login")
    suspend fun login(
        request: LoginRequest,
        @HeaderParam("User-Agent") userAgent: String?,
        @HeaderParam("X-Forwarded-For") forwardedFor: String?,
    ): TokenPairResponse =
        authenticationService.login(
            request.toCommand(
                userAgent = userAgent,
                forwardedFor = forwardedFor,
            ),
        ).toResponse()

    /**
     * 使用 refresh token 轮换一对新令牌。
     *
     * @param request 包含 refresh token 明文的请求体。
     * @return 刷新后的 access token 与 refresh token。
     */
    @POST
    @Path("/refresh")
    suspend fun refresh(request: RefreshTokenRequest): TokenPairResponse =
        authenticationService.refresh(request.toRefreshToken()).toResponse()

    /**
     * 注销当前会话。
     *
     * @return `204 No Content`。
     */
    @POST
    @Path("/logout")
    @Authenticated
    suspend fun logout(): Response {
        authenticationService.logout(jwt.toAuthenticatedSessionPrincipal())
        return Response.noContent().build()
    }

    /**
     * 注销当前用户的全部会话。
     *
     * @return `204 No Content`。
     */
    @POST
    @Path("/logout-all")
    @Authenticated
    suspend fun logoutAll(): Response {
        authenticationService.logoutAll(jwt.toAuthenticatedSessionPrincipal())
        return Response.noContent().build()
    }

    /**
     * 查询当前登录用户及其授权快照。
     *
     * @return 当前会话绑定的用户信息、角色、权限和菜单树。
     */
    @GET
    @Path("/current-user")
    @Authenticated
    suspend fun currentUser(): CurrentUserResponse =
        authenticationService.currentUser(jwt.toAuthenticatedSessionPrincipal()).toResponse()

    /**
     * 列出当前用户的可见会话。
     *
     * @return 会话列表，并额外标记哪一个是当前请求所在会话。
     */
    @GET
    @Path("/sessions")
    @Authenticated
    suspend fun listSessions(): List<AuthSessionResponse> {
        val principal = jwt.toAuthenticatedSessionPrincipal()
        return authenticationService.listSessions(principal).toResponses(principal.sessionId)
    }

    /**
     * 撤销指定会话。
     *
     * @param sessionId 目标会话键。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/sessions/{sessionId}")
    @Authenticated
    suspend fun deleteSession(
        @PathParam("sessionId") sessionId: String,
    ): Response {
        authenticationService.revokeSession(jwt.toAuthenticatedSessionPrincipal(), sessionId)
        return Response.noContent().build()
    }
}