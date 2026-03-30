package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.MenuTreeView
import io.github.lishangbu.avalon.authorization.entity.dto.MenuView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveMenuInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateMenuInput
import io.github.lishangbu.avalon.authorization.service.MenuService
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
    ): List<MenuTreeView> = menuService.listMenuTreeByRoleCodes(user.authorities.mapNotNull { it.authority })

    /** 查询全量菜单树 */
    @GetMapping("/tree")
    fun listTree(): List<MenuTreeView> = menuService.listTree()

    /** 按条件查询菜单列表 */
    @GetMapping("/list")
    fun listByCondition(
        @ModelAttribute specification: MenuSpecification,
    ): List<MenuView> = menuService.listByCondition(specification)

    /**
     * 根据 ID 查询菜单
     *
     * @param id 菜单 ID
     * @return 菜单信息
     */
    @GetMapping("/{id:\\d+}")
    fun getById(
        @PathVariable id: Long,
    ): MenuView? = menuService.getById(id)

    /**
     * 新增菜单
     *
     * @param input 菜单写入请求
     * @return 保存后的菜单
     */
    @PostMapping
    fun save(
        @RequestBody @Valid input: SaveMenuInput,
    ): MenuView = menuService.save(input)

    /**
     * 更新菜单
     *
     * @param input 菜单写入请求
     * @return 更新后的菜单
     */
    @PutMapping
    fun update(
        @RequestBody @Valid input: UpdateMenuInput,
    ): MenuView = menuService.update(input)

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
