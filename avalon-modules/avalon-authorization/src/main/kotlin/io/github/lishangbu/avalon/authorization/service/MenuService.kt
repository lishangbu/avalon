package io.github.lishangbu.avalon.authorization.service

import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.MenuTreeView
import io.github.lishangbu.avalon.authorization.entity.dto.MenuView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveMenuInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateMenuInput

/**
 * 菜单服务接口
 *
 * 提供根据角色获取菜单树的能力
 *
 * @author lishangbu
 * @since 2025/8/28
 */
interface MenuService {
    /**
     * 根据角色代码获取菜单树
     *
     * @param roleCodes 角色代码
     * @return 菜单树视图列表
     */
    fun listMenuTreeByRoleCodes(roleCodes: List<String>): List<MenuTreeView>

    /** 查询全量菜单树 */
    fun listTree(): List<MenuTreeView>

    /** 按条件查询菜单列表 */
    fun listByCondition(specification: MenuSpecification): List<MenuView>

    /** 按 ID 查询菜单 */
    fun getById(id: Long): MenuView?

    /** 保存菜单 */
    fun save(command: SaveMenuInput): MenuView

    /** 更新菜单 */
    fun update(command: UpdateMenuInput): MenuView

    /** 按 ID 删除菜单 */
    fun removeById(id: Long)
}
