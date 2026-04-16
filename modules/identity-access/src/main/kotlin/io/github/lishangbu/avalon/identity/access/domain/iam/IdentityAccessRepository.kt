package io.github.lishangbu.avalon.identity.access.domain.iam

/**
 * IdentityAccess 写模型与授权快照的仓储契约。
 *
 * 这个契约负责用户、角色、权限、菜单以及授权上下文的持久化读写边界。
 * 上层应用服务只依赖这里定义的领域契约，不直接感知 SQL、表结构或连接表装配细节。
 */
interface IdentityAccessRepository {
    /**
     * 按用户标识查询用户。
     *
     * @param id 用户标识。
     * @return 命中的用户；如果不存在则返回 `null`。
     */
    suspend fun findUser(id: UserId): UserAccount?

    /**
     * 创建用户并返回创建后的聚合状态。
     *
     * @param draft 用户草稿，承载用户名、显示名、启用状态和角色绑定等输入。
     * @return 已持久化的用户聚合。
     */
    suspend fun createUser(draft: UserAccountDraft): UserAccount

    /**
     * 更新指定用户并返回更新后的聚合状态。
     *
     * @param id 要更新的用户标识。
     * @param draft 更新草稿，未通过校验或冲突的情况由实现抛出领域异常。
     * @return 更新后的用户聚合。
     */
    suspend fun updateUser(
        id: UserId,
        draft: UserAccountDraft,
    ): UserAccount

    /**
     * 删除指定用户。
     *
     * @param id 要删除的用户标识。
     */
    suspend fun deleteUser(id: UserId)

    /**
     * 按角色标识查询角色。
     *
     * @param id 角色标识。
     * @return 命中的角色；如果不存在则返回 `null`。
     */
    suspend fun findRole(id: RoleId): Role?

    /**
     * 创建角色。
     *
     * @param draft 角色草稿，包含角色编码、名称与关联权限。
     * @return 已持久化的角色聚合。
     */
    suspend fun createRole(draft: RoleDraft): Role

    /**
     * 更新角色。
     *
     * @param id 要更新的角色标识。
     * @param draft 角色草稿，承载更新后的角色属性与权限绑定。
     * @return 更新后的角色聚合。
     */
    suspend fun updateRole(
        id: RoleId,
        draft: RoleDraft,
    ): Role

    /**
     * 删除指定角色。
     *
     * @param id 要删除的角色标识。
     */
    suspend fun deleteRole(id: RoleId)

    /**
     * 按权限标识查询权限。
     *
     * @param id 权限标识。
     * @return 命中的权限；如果不存在则返回 `null`。
     */
    suspend fun findPermission(id: PermissionId): Permission?

    /**
     * 创建权限。
     *
     * @param draft 权限草稿，包含权限编码、名称和说明。
     * @return 已持久化的权限聚合。
     */
    suspend fun createPermission(draft: PermissionDraft): Permission

    /**
     * 更新权限。
     *
     * @param id 要更新的权限标识。
     * @param draft 权限草稿，承载更新后的权限属性。
     * @return 更新后的权限聚合。
     */
    suspend fun updatePermission(
        id: PermissionId,
        draft: PermissionDraft,
    ): Permission

    /**
     * 删除指定权限。
     *
     * @param id 要删除的权限标识。
     */
    suspend fun deletePermission(id: PermissionId)

    /**
     * 列出全部菜单定义。
     *
     * @return 菜单列表；若无数据则返回空列表。
     */
    suspend fun listMenus(): List<Menu>

    /**
     * 按菜单标识查询菜单。
     *
     * @param id 菜单标识。
     * @return 命中的菜单；如果不存在则返回 `null`。
     */
    suspend fun findMenu(id: MenuId): Menu?

    /**
     * 创建菜单。
     *
     * @param draft 菜单草稿，包含层级关系、路由信息、展示状态和绑定权限。
     * @return 已持久化的菜单聚合。
     */
    suspend fun createMenu(draft: MenuDraft): Menu

    /**
     * 更新菜单。
     *
     * @param id 要更新的菜单标识。
     * @param draft 菜单草稿，承载更新后的层级、路由和权限绑定关系。
     * @return 更新后的菜单聚合。
     */
    suspend fun updateMenu(
        id: MenuId,
        draft: MenuDraft,
    ): Menu

    /**
     * 删除指定菜单。
     *
     * @param id 要删除的菜单标识。
     */
    suspend fun deleteMenu(id: MenuId)

    /**
     * 组装用户当前授权上下文。
     *
     * 这个查询用于认证后的授权快照构建，因此实现通常会一次性装配用户、角色、
     * 权限和菜单集合，避免上层重复往返数据库。
     *
     * @param userId 用户标识。
     * @return 对应用户的授权上下文；如果用户不存在则返回 `null`。
     */
    suspend fun findAuthorizationContext(userId: UserId): AuthorizationContext?
}