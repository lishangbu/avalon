package io.github.lishangbu.avalon.authorization.service

import io.github.lishangbu.avalon.authorization.entity.Role
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/**
 * 角色服务
 *
 * 定义与角色相关的业务契约
 *
 * @author lishangbu
 * @since 2025/8/30
 */
interface RoleService {
    /** 按条件分页查询角色 */
    fun getPageByCondition(
        role: Role,
        pageable: Pageable,
    ): Page<Role>

    /** 按条件查询角色列表 */
    fun listByCondition(role: Role): List<Role>

    /** 按 ID 查询角色 */
    fun getById(id: Long): Role?

    /** 保存角色 */
    fun save(role: Role): Role

    /** 更新角色 */
    fun update(role: Role): Role

    /** 按 ID 删除角色 */
    fun removeById(id: Long)
}
