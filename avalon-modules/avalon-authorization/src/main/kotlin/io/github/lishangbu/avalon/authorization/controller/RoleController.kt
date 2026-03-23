package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.service.RoleService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/**
 * 角色控制器
 *
 * 提供角色管理相关接口
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@RequestMapping("/role")
@RestController
class RoleController(
    /** 角色服务 */
    private val roleService: RoleService,
) {
    /**
     * 分页条件查询角色
     *
     * @param pageable 分页参数
     * @param role 查询条件
     * @return 角色分页结果
     */
    @GetMapping("/page")
    fun getRolePage(
        pageable: Pageable,
        role: Role,
    ): Page<Role> = roleService.getPageByCondition(role, pageable)

    /**
     * 条件查询角色列表
     *
     * @param role 查询条件
     * @return 角色列表
     */
    @GetMapping("/list")
    fun listRoles(role: Role): List<Role> = roleService.listByCondition(role)

    /**
     * 根据 ID 查询角色
     *
     * @param id 角色 ID
     * @return 角色信息
     */
    @GetMapping("/{id:\\d+}")
    fun getById(
        @PathVariable id: Long,
    ): Role? = roleService.getById(id)

    /**
     * 新增角色
     *
     * @param role 角色实体
     * @return 保存后的角色
     */
    @PostMapping
    fun save(
        @RequestBody role: Role,
    ): Role = roleService.save(role)

    /**
     * 更新角色
     *
     * @param role 角色实体
     * @return 更新后的角色
     */
    @PutMapping
    fun update(
        @RequestBody role: Role,
    ): Role = roleService.update(role)

    /**
     * 根据 ID 删除角色
     *
     * @param id 角色 ID
     */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        roleService.removeById(id)
    }
}
