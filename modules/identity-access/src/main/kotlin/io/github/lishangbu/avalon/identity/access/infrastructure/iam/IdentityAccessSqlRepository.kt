package io.github.lishangbu.avalon.identity.access.infrastructure.iam

import io.github.lishangbu.avalon.identity.access.application.iam.query.*
import io.github.lishangbu.avalon.identity.access.domain.iam.*
import io.github.lishangbu.avalon.shared.application.query.Page
import io.github.lishangbu.avalon.shared.infra.sql.translateSqlErrors
import io.vertx.mutiny.sqlclient.Pool
import jakarta.enterprise.context.ApplicationScoped

/**
 * IAM SQL 仓储统一承接写模型仓储与分页查询端口的基础设施实现。
 *
 * application 层分别依赖 `IdentityAccessRepository` 与 `IdentityAccessPageQueryRepository`
 * 这两个契约；基础设施层在这里把用户、角色、权限、菜单和授权快照相关的 SQL
 * 协作者组装起来，对外保持一个稳定的 CDI 入口，同时避免把全部 SQL 细节继续堆进
 * 同一个超大 gateway 或 service。
 */
@ApplicationScoped
class IdentityAccessSqlRepository(
    pool: Pool,
) : IdentityAccessRepository, IdentityAccessPageQueryRepository {
    private val userGateway = UserSqlGateway(pool)
    private val roleGateway = RoleSqlGateway(pool)
    private val permissionGateway = PermissionSqlGateway(pool)
    private val menuGateway = MenuSqlGateway(pool)
    private val authorizationContextQuery =
        AuthorizationContextSqlQuery(
            userGateway = userGateway,
            roleGateway = roleGateway,
            permissionGateway = permissionGateway,
            menuGateway = menuGateway,
        )

    override suspend fun pageUsers(query: UserPageQuery): Page<UserAccount> =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { userGateway.pageUsers(query) }

    override suspend fun findUser(id: UserId): UserAccount? =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { userGateway.findUser(id) }

    override suspend fun createUser(draft: UserAccountDraft): UserAccount =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { userGateway.createUser(draft) }

    override suspend fun updateUser(
        id: UserId,
        draft: UserAccountDraft,
    ): UserAccount =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { userGateway.updateUser(id, draft) }

    override suspend fun deleteUser(id: UserId) {
        translateSqlErrors(::mapIdentityAccessDatabaseError) { userGateway.deleteUser(id) }
    }

    override suspend fun pageRoles(query: RolePageQuery): Page<Role> =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { roleGateway.pageRoles(query) }

    override suspend fun listRoles(query: RoleListQuery): List<Role> =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { roleGateway.listRoles(query) }

    override suspend fun findRole(id: RoleId): Role? =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { roleGateway.findRole(id) }

    override suspend fun createRole(draft: RoleDraft): Role =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { roleGateway.createRole(draft) }

    override suspend fun updateRole(
        id: RoleId,
        draft: RoleDraft,
    ): Role =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { roleGateway.updateRole(id, draft) }

    override suspend fun deleteRole(id: RoleId) {
        translateSqlErrors(::mapIdentityAccessDatabaseError) { roleGateway.deleteRole(id) }
    }

    override suspend fun pagePermissions(query: PermissionPageQuery): Page<Permission> =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { permissionGateway.pagePermissions(query) }

    override suspend fun listPermissions(query: PermissionListQuery): List<Permission> =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { permissionGateway.listPermissions(query) }

    override suspend fun findPermission(id: PermissionId): Permission? =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { permissionGateway.findPermission(id) }

    override suspend fun createPermission(draft: PermissionDraft): Permission =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { permissionGateway.createPermission(draft) }

    override suspend fun updatePermission(
        id: PermissionId,
        draft: PermissionDraft,
    ): Permission =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { permissionGateway.updatePermission(id, draft) }

    override suspend fun deletePermission(id: PermissionId) {
        translateSqlErrors(::mapIdentityAccessDatabaseError) { permissionGateway.deletePermission(id) }
    }

    override suspend fun pageMenus(query: MenuPageQuery): Page<Menu> =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { menuGateway.pageMenus(query.pageRequest) }

    override suspend fun listMenus(): List<Menu> =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { menuGateway.listMenus() }

    override suspend fun findMenu(id: MenuId): Menu? =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { menuGateway.findMenu(id) }

    override suspend fun createMenu(draft: MenuDraft): Menu =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { menuGateway.createMenu(draft) }

    override suspend fun updateMenu(
        id: MenuId,
        draft: MenuDraft,
    ): Menu =
        translateSqlErrors(::mapIdentityAccessDatabaseError) { menuGateway.updateMenu(id, draft) }

    override suspend fun deleteMenu(id: MenuId) {
        translateSqlErrors(::mapIdentityAccessDatabaseError) { menuGateway.deleteMenu(id) }
    }

    override suspend fun findAuthorizationContext(userId: UserId): AuthorizationContext? =
        translateSqlErrors(::mapIdentityAccessDatabaseError) {
            authorizationContextQuery.findAuthorizationContext(userId)
        }
}