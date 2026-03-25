package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.Menu
import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
import io.github.lishangbu.avalon.authorization.model.MenuTreeNode
import io.github.lishangbu.avalon.authorization.service.MenuService
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 菜单控制器
 *
 * 提供当前用户基于角色的菜单树查询接口
 *
 * @author lishangbu
 * @since 2025/8/28
 */
@RequestMapping("/menu")
@RestController
class MenuController(
    /** 菜单服务 */
    private val menuService: MenuService,
) {
    /**
     * 获取当前用户角色菜单树
     *
     * @param user 当前用户
     * @return 菜单树
     */
    @GetMapping("/role-tree")
    fun listCurrentRoleMenuTree(
        @AuthenticationPrincipal user: UserInfo,
    ): List<MenuTreeNode> = menuService.listMenuTreeByRoleCodes(user.authorities.mapNotNull { it.authority })

    /**
     * 查询全量菜单树（支持按完整菜单条件筛选）
     *
     * @param specification 菜单查询条件
     * @return 菜单树
     */
    @GetMapping("/tree")
    fun listAllMenuTree(
        @ModelAttribute specification: MenuSpecification,
    ): List<MenuTreeNode> = menuService.listAllMenuTree(specification)

    /**
     * 根据 ID 查询菜单
     *
     * @param id 菜单 ID
     * @return 菜单信息
     */
    @GetMapping("/{id:\\d+}")
    fun getById(
        @PathVariable id: Long,
    ): Menu? = menuService.getById(id)

    /**
     * 新增菜单
     *
     * @param menu 菜单实体
     * @return 保存后的菜单
     */
    @PostMapping
    fun save(
        @RequestBody menu: Menu,
    ): Menu = menuService.save(menu)

    /**
     * 更新菜单
     *
     * @param menu 菜单实体
     * @return 更新后的菜单
     */
    @PutMapping
    fun update(
        @RequestBody menu: Menu,
    ): Menu = menuService.update(menu)

    /**
     * 根据 ID 删除菜单
     *
     * @param id 菜单 ID
     */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        menuService.removeById(id)
    }
}
