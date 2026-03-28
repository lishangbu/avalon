package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.dto.RoleSpecification
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.data.domain.Pageable

/**
 * 角色仓储接口
 *
 * 定义角色数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/08/20
 */
interface RoleRepository :
    KRepository<Role, Long>,
    RoleRepositoryExt

interface RoleRepositoryExt {
    /** 按条件查询角色列表 */
    fun findAll(specification: RoleSpecification?): List<Role>

    /** 按条件分页查询角色 */
    fun findAll(
        specification: RoleSpecification?,
        pageable: Pageable,
    ): Page<Role>

    /** 按条件查询角色列表，并抓取菜单 */
    fun findAllWithMenus(specification: RoleSpecification?): List<Role>

    /** 按条件分页查询角色，并抓取菜单 */
    fun findAllWithMenus(
        specification: RoleSpecification?,
        pageable: Pageable,
    ): Page<Role>

    /** 按 ID 删除角色 */
    fun removeById(id: Long)
}
