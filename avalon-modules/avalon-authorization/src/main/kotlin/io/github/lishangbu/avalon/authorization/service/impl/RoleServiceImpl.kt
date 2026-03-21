package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.authorization.service.RoleService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * 角色信息服务实现类
 *
 * 提供对角色数据的查询与管理（实现 RoleService）
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@Service
class RoleServiceImpl(
    private val roleRepository: RoleRepository,
    private val menuRepository: MenuRepository,
) : RoleService {
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

    override fun getById(id: Long): Optional<Role> = roleRepository.findById(id)

    @Transactional(rollbackFor = [Exception::class])
    override fun save(role: Role): Role {
        val prepared = bindMenus(role, false)
        return roleRepository.save(prepared)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun update(role: Role): Role {
        val prepared = bindMenus(role, true)
        return roleRepository.save(prepared)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        roleRepository.deleteById(id)
    }

    private fun bindMenus(
        role: Role,
        preserveWhenNull: Boolean,
    ): Role {
        val existing =
            if (preserveWhenNull) {
                role.readOrNull { id }?.let { roleRepository.findById(it).orElse(null) }
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

    private inline fun <T, R> T?.readOrNull(block: T.() -> R): R? = this?.let { runCatching { it.block() }.getOrNull() }
}
