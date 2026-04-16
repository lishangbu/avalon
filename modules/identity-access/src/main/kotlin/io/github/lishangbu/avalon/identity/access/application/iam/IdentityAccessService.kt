package io.github.lishangbu.avalon.identity.access.application.iam

import io.github.lishangbu.avalon.identity.access.application.iam.query.*
import io.github.lishangbu.avalon.identity.access.domain.iam.*
import io.github.lishangbu.avalon.shared.application.query.Page
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
/**
 * IdentityAccess 的应用服务入口。
 *
 * 这个服务负责把接口层使用的基础标识类型转换为领域标识，
 * 并协调用户、角色、权限、菜单以及授权快照查询。
 * 具体持久化与复杂装配细节仍由仓储实现承担。
 */
class IdentityAccessService(
    private val repository: IdentityAccessRepository,
    private val pageQueryRepository: IdentityAccessPageQueryRepository,
) {
    /**
     * 按固定排序分页列出用户。
     *
     * 当前保持按 `id` 的稳定顺序返回结果，避免页间漂移。
     *
     * @param query 用户分页查询条件。
     * @return 当前页用户与分页元数据。
     */
    suspend fun pageUsers(query: UserPageQuery): Page<UserAccount> = pageQueryRepository.pageUsers(query)

    /**
     * 查询单个用户。
     *
     * @param id 用户主键值。
     * @return 命中的用户。
     * @throws IdentityAccessNotFound 当用户不存在时抛出。
     */
    suspend fun getUser(id: UUID): UserAccount =
        repository.findUser(UserId(id)) ?: throw IdentityAccessNotFound("user", id.toString())

    /**
     * 创建用户。
     *
     * @param draft 用户草稿。
     * @return 已持久化的用户聚合。
     */
    suspend fun createUser(draft: UserAccountDraft): UserAccount = repository.createUser(draft)

    /**
     * 更新用户。
     *
     * @param id 用户主键值。
     * @param draft 用户更新草稿。
     * @return 更新后的用户聚合。
     */
    suspend fun updateUser(
        id: UUID,
        draft: UserAccountDraft,
    ): UserAccount = repository.updateUser(UserId(id), draft)

    /**
     * 删除用户。
     *
     * @param id 用户主键值。
     */
    suspend fun deleteUser(id: UUID) {
        repository.deleteUser(UserId(id))
    }

    /**
     * 按固定排序分页列出角色。
     *
     * 当前保持按 `code, id` 的稳定顺序返回结果，并支持按编码、名称和启用状态筛选。
     *
     * @param query 角色分页查询条件。
     * @return 当前页角色与分页元数据。
     */
    suspend fun pageRoles(query: RolePageQuery): Page<Role> = pageQueryRepository.pageRoles(query)

    /**
     * 按固定排序列出角色。
     *
     * @param query 角色列表查询条件。
     * @return 命中的角色集合。
     */
    suspend fun listRoles(query: RoleListQuery): List<Role> = pageQueryRepository.listRoles(query)

    /**
     * 查询单个角色。
     *
     * @param id 角色主键值。
     * @return 命中的角色。
     * @throws IdentityAccessNotFound 当角色不存在时抛出。
     */
    suspend fun getRole(id: UUID): Role =
        repository.findRole(RoleId(id)) ?: throw IdentityAccessNotFound("role", id.toString())

    /**
     * 创建角色。
     *
     * @param draft 角色草稿。
     * @return 已持久化的角色聚合。
     */
    suspend fun createRole(draft: RoleDraft): Role = repository.createRole(draft)

    /**
     * 更新角色。
     *
     * @param id 角色主键值。
     * @param draft 角色更新草稿。
     * @return 更新后的角色聚合。
     */
    suspend fun updateRole(
        id: UUID,
        draft: RoleDraft,
    ): Role = repository.updateRole(RoleId(id), draft)

    /**
     * 删除角色。
     *
     * @param id 角色主键值。
     */
    suspend fun deleteRole(id: UUID) {
        repository.deleteRole(RoleId(id))
    }

    /**
     * 按固定排序分页列出权限。
     *
     * 当前保持按 `sortingOrder, id` 的稳定顺序返回结果，并支持按菜单、编码、名称和启用状态筛选。
     *
     * @param query 权限分页查询条件。
     * @return 当前页权限与分页元数据。
     */
    suspend fun pagePermissions(query: PermissionPageQuery): Page<Permission> =
        pageQueryRepository.pagePermissions(query)

    /**
     * 按固定排序列出权限。
     *
     * @param query 权限列表查询条件。
     * @return 命中的权限集合。
     */
    suspend fun listPermissions(query: PermissionListQuery): List<Permission> =
        pageQueryRepository.listPermissions(query)

    /**
     * 查询单个权限。
     *
     * @param id 权限主键值。
     * @return 命中的权限。
     * @throws IdentityAccessNotFound 当权限不存在时抛出。
     */
    suspend fun getPermission(id: UUID): Permission =
        repository.findPermission(PermissionId(id)) ?: throw IdentityAccessNotFound("permission", id.toString())

    /**
     * 创建权限。
     *
     * @param draft 权限草稿。
     * @return 已持久化的权限聚合。
     */
    suspend fun createPermission(draft: PermissionDraft): Permission = repository.createPermission(draft)

    /**
     * 更新权限。
     *
     * @param id 权限主键值。
     * @param draft 权限更新草稿。
     * @return 更新后的权限聚合。
     */
    suspend fun updatePermission(
        id: UUID,
        draft: PermissionDraft,
    ): Permission = repository.updatePermission(PermissionId(id), draft)

    /**
     * 删除权限。
     *
     * @param id 权限主键值。
     */
    suspend fun deletePermission(id: UUID) {
        repository.deletePermission(PermissionId(id))
    }

    /**
     * 按固定排序分页列出平铺菜单。
     *
     * 当前保持按 `sortingOrder, id` 的稳定顺序返回结果。
     *
     * @param query 平铺菜单分页查询条件。
     * @return 当前页菜单与分页元数据。
     */
    suspend fun pageMenus(query: MenuPageQuery): Page<Menu> = pageQueryRepository.pageMenus(query)

    /**
     * 查询单个菜单。
     *
     * @param id 菜单主键值。
     * @return 命中的菜单。
     * @throws IdentityAccessNotFound 当菜单不存在时抛出。
     */
    suspend fun getMenu(id: UUID): Menu =
        repository.findMenu(MenuId(id)) ?: throw IdentityAccessNotFound("menu", id.toString())

    /**
     * 创建菜单。
     *
     * @param draft 菜单草稿。
     * @return 已持久化的菜单聚合。
     */
    suspend fun createMenu(draft: MenuDraft): Menu = repository.createMenu(draft)

    /**
     * 更新菜单。
     *
     * @param id 菜单主键值。
     * @param draft 菜单更新草稿。
     * @return 更新后的菜单聚合。
     */
    suspend fun updateMenu(
        id: UUID,
        draft: MenuDraft,
    ): Menu = repository.updateMenu(MenuId(id), draft)

    /**
     * 删除菜单。
     *
     * @param id 菜单主键值。
     */
    suspend fun deleteMenu(id: UUID) {
        repository.deleteMenu(MenuId(id))
    }

    /**
     * 返回树形菜单结构。
     *
     * 设计上菜单树构建保持在应用层，是因为它是对领域菜单集合的读取侧装配，
     * 不需要反向写回领域状态。
     *
     * @return 按排序规则组织好的根节点列表。
     */
    suspend fun listMenuTree(): List<MenuTreeNode> = buildMenuTree(repository.listMenus())

    /**
     * 构建用户当前授权快照。
     *
     * 这个用例会把用户、角色、权限和平铺菜单集合装配成可直接对外返回的授权视图，
     * 避免接口层自行拼装授权树。
     *
     * @param userId 用户主键值。
     * @return 当前用户的授权快照。
     * @throws IdentityAccessNotFound 当用户不存在时抛出。
     */
    suspend fun getAuthorizationSnapshot(userId: UUID): AuthorizationSnapshot {
        val context =
            repository.findAuthorizationContext(UserId(userId))
                ?: throw IdentityAccessNotFound("user", userId.toString())
        return AuthorizationSnapshot(
            user = context.user,
            roles = context.roles,
            permissions = context.permissions,
            menuTree = buildMenuTree(context.menus),
        )
    }

    /**
     * 把平铺菜单集合组装成树结构。
     *
     * @param menus 平铺菜单集合。
     * @return 以 `parentId == null` 为根的菜单树。
     */
    private fun buildMenuTree(menus: List<Menu>): List<MenuTreeNode> {
        val sortedMenus = menus.sortedWith(compareBy(Menu::sortingOrder, { it.id.value }))
        val childrenByParent = sortedMenus.groupBy(Menu::parentId)

        fun toNode(menu: Menu): MenuTreeNode =
            MenuTreeNode(
                id = menu.id,
                parentId = menu.parentId,
                key = menu.key,
                title = menu.title,
                visible = menu.visible,
                path = menu.path,
                routeName = menu.routeName,
                component = menu.component,
                icon = menu.icon,
                sortingOrder = menu.sortingOrder,
                type = menu.type,
                hidden = menu.hidden,
                disabled = menu.disabled,
                external = menu.external,
                target = menu.target,
                children = childrenByParent[menu.id].orEmpty().map(::toNode),
            )

        return childrenByParent[null].orEmpty().map(::toNode)
    }
}

