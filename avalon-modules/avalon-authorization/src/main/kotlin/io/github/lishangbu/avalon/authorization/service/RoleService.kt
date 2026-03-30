package io.github.lishangbu.avalon.authorization.service

import io.github.lishangbu.avalon.authorization.entity.dto.RoleSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.RoleView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveRoleInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateRoleInput
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
        specification: RoleSpecification,
        pageable: Pageable,
    ): Page<RoleView>

    /** 按条件查询角色列表 */
    fun listByCondition(specification: RoleSpecification): List<RoleView>

    /** 按 ID 查询角色 */
    fun getById(id: Long): RoleView?

    /** 保存角色 */
    fun save(command: SaveRoleInput): RoleView

    /** 更新角色 */
    fun update(command: UpdateRoleInput): RoleView

    /** 按 ID 删除角色 */
    fun removeById(id: Long)
}
