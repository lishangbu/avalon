package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.dto.RoleSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.RoleView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveRoleInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateRoleInput
import io.github.lishangbu.avalon.authorization.service.RoleService
import jakarta.validation.Valid
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
        @ModelAttribute specification: RoleSpecification,
    ): Page<RoleView> = roleService.getPageByCondition(specification, pageable)

    /**
     * 条件查询角色列表
     *
     * @param role 查询条件
     * @return 角色列表
     */
    @GetMapping("/list")
    fun listRoles(
        @ModelAttribute specification: RoleSpecification,
    ): List<RoleView> = roleService.listByCondition(specification)

    /**
     * 根据 ID 查询角色
     *
     * @param id 角色 ID
     * @return 角色信息
     */
    @GetMapping("/{id:\\d+}")
    fun getById(
        @PathVariable id: Long,
    ): RoleView? = roleService.getById(id)

    /**
     * 新增角色
     *
     * @param input 角色写入请求
     * @return 保存后的角色
     */
    @PostMapping
    fun save(
        @RequestBody @Valid input: SaveRoleInput,
    ): RoleView = roleService.save(input)

    /**
     * 更新角色
     *
     * @param input 角色写入请求
     * @return 更新后的角色
     */
    @PutMapping
    fun update(
        @RequestBody @Valid input: UpdateRoleInput,
    ): RoleView = roleService.update(input)

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
