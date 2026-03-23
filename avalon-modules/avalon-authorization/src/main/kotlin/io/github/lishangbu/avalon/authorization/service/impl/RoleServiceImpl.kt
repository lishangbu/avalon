package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.addBy
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.authorization.repository.readOrNull
import io.github.lishangbu.avalon.authorization.service.RoleService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
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
        role: Role,
        pageable: Pageable,
    ): Page<Role> =
        roleRepository.findAll(
            Example.of(
                role,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("code", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
            pageable,
        )

    /** 根据条件查询角色列表 */
    override fun listByCondition(role: Role): List<Role> =
        roleRepository.findAll(
            Example.of(
                role,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("code", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
        )

    /** 按 ID 查询角色 */
    override fun getById(id: Long): Role? = roleRepository.findById(id)

    /** 保存角色 */
    @Transactional(rollbackFor = [Exception::class])
    override fun save(role: Role): Role {
        val prepared = bindMenus(role, false)
        return roleRepository.save(prepared)
    }

    /** 更新角色 */
    @Transactional(rollbackFor = [Exception::class])
    override fun update(role: Role): Role {
        val prepared = bindMenus(role, true)
        return roleRepository.save(prepared)
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
                role.readOrNull { id }?.let(roleRepository::findById)
            } else {
                null
            }

        val currentMenus = role.readOrNull { menus } ?: emptyList()
        val menuIds = currentMenus.mapNotNull { it.readOrNull { id } }.toCollection(LinkedHashSet())
        val boundMenus =
            when {
                menuIds.isNotEmpty() -> menuRepository.findAllById(menuIds)
                preserveWhenNull -> existing?.readOrNull { menus } ?: emptyList()
                else -> emptyList()
            }

        return Role {
            role.readOrNull { id }?.let { id = it }
            code = role.readOrNull { code } ?: existing?.readOrNull { code }
            name = role.readOrNull { name } ?: existing?.readOrNull { name }
            enabled = role.readOrNull { enabled } ?: existing?.readOrNull { enabled }
            boundMenus.forEach { boundMenu -> menus().addBy(boundMenu) }
        }
    }
}
