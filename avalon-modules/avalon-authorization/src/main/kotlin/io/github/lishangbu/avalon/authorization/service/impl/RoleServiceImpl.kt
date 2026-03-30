package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.addBy
import io.github.lishangbu.avalon.authorization.entity.dto.RoleSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.RoleView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveRoleInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateRoleInput
import io.github.lishangbu.avalon.authorization.repository.AuthorizationFetchers
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.authorization.service.RoleService
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 角色服务实现
 *
 * 负责角色及其菜单关联的查询与维护
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@Service
class RoleServiceImpl(
    /** 角色仓储 */
    private val roleRepository: RoleRepository,
    /** 菜单仓储 */
    private val menuRepository: MenuRepository,
) : RoleService {
    /** 按条件分页查询角色 */
    override fun getPageByCondition(
        specification: RoleSpecification,
        pageable: Pageable,
    ): Page<RoleView> = roleRepository.pageViews(specification, pageable)

    /** 根据条件查询角色列表 */
    override fun listByCondition(specification: RoleSpecification): List<RoleView> = roleRepository.listViews(specification)

    /** 按 ID 查询角色 */
    override fun getById(id: Long): RoleView? = roleRepository.loadViewById(id)

    /** 保存角色 */
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveRoleInput): RoleView {
        val prepared = bindMenus(command.toEntity(), false)
        return roleRepository.save(prepared, SaveMode.INSERT_ONLY).let(::reloadView)
    }

    /** 更新角色 */
    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateRoleInput): RoleView {
        val prepared = bindMenus(command.toEntity(), true)
        return roleRepository.save(prepared).let(::reloadView)
    }

    /** 按 ID 删除角色 */
    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        roleRepository.deleteById(id)
    }

    /** 返回绑定菜单列表 */
    private fun bindMenus(
        role: Role,
        preserveWhenNull: Boolean,
    ): Role {
        val existing =
            if (preserveWhenNull) {
                role.readOrNull { id }?.let { roleId -> roleRepository.findNullable(roleId, AuthorizationFetchers.ROLE_WITH_MENUS) }
            } else {
                null
            }

        val currentMenus = role.readOrNull { menus }
        val menuIds = currentMenus?.mapNotNull { it.readOrNull { id } }?.toCollection(LinkedHashSet())
        val shouldLoadMenus = currentMenus != null
        val boundMenus =
            when {
                currentMenus != null && !menuIds.isNullOrEmpty() -> menuRepository.findAllById(menuIds)
                currentMenus != null -> emptyList()
                preserveWhenNull -> existing?.readOrNull { menus } ?: emptyList()
                else -> emptyList()
            }

        return Role {
            role.readOrNull { id }?.let { id = it }
            code = role.readOrNull { code } ?: existing?.readOrNull { code }
            name = role.readOrNull { name } ?: existing?.readOrNull { name }
            enabled = role.readOrNull { enabled } ?: existing?.readOrNull { enabled }
            if (shouldLoadMenus) {
                menus()
            }
            boundMenus.forEach { boundMenu -> menus().addBy(boundMenu) }
        }
    }

    private fun reloadView(role: Role): RoleView = requireNotNull(roleRepository.loadViewById(role.id)) { "未找到 ID=${role.id} 对应的角色" }
}
