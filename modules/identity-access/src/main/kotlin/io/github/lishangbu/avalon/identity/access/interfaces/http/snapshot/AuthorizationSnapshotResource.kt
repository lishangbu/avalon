package io.github.lishangbu.avalon.identity.access.interfaces.http.snapshot

import io.github.lishangbu.avalon.identity.access.application.iam.IdentityAccessService
import io.github.lishangbu.avalon.identity.access.interfaces.http.iam.AuthorizationSnapshotResponse
import io.github.lishangbu.avalon.identity.access.interfaces.http.iam.toResponse
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import java.util.UUID

/**
 * 对外提供授权快照查询的 HTTP 入口。
 */
@Path("/iam/authorization-snapshots")
@Produces(MediaType.APPLICATION_JSON)
class AuthorizationSnapshotResource(
    private val service: IdentityAccessService,
) {
    /**
     * 查询指定用户的授权快照。
     *
     * @param userId 用户主键值。
     * @return 用户、角色、权限和菜单树组成的授权快照。
     */
    @GET
    @Path("/users/{userId}")
    suspend fun getUserAuthorizationSnapshot(
        @PathParam("userId") userId: UUID,
    ): AuthorizationSnapshotResponse = service.getAuthorizationSnapshot(userId).toResponse()
}

