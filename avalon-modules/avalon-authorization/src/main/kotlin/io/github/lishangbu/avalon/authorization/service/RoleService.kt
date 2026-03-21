package io.github.lishangbu.avalon.authorization.service

import io.github.lishangbu.avalon.authorization.entity.Role
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import java.util.*

/**
 * 角色服务
 *
 * 定义与角色相关的业务契约
 *
 * @author lishangbu
 * @since 2025/8/30
 */
interface RoleService {
    /** 根据条件分页查询角色。 */
    fun getPageByCondition(
        role: Role,
        pageable: Pageable,
    ): Page<Role>

    /** 根据条件查询角色列表。 */
    fun listByCondition(role: Role): List<Role>

    /** 根据 ID 查询角色。 */
    fun getById(id: Long): Optional<Role>

    /** 新增角色。 */
    fun save(role: Role): Role

    /** 更新角色。 */
    fun update(role: Role): Role

    /** 根据 ID 删除角色。 */
    fun removeById(id: Long)
}
