package io.github.lishangbu.avalon.authorization.service

import io.github.lishangbu.avalon.authorization.entity.Menu
import io.github.lishangbu.avalon.authorization.model.MenuTreeNode

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
     * @return 菜单树节点列表
     */
    fun listMenuTreeByRoleCodes(roleCodes: List<String>): List<MenuTreeNode>

    /**
     * 查询全量菜单树（支持按完整菜单条件筛选）
     *
     * @param menu 菜单查询条件，可为空
     * @return 菜单树节点列表
     */
    fun listAllMenuTree(menu: Menu): List<MenuTreeNode>

    /** 按 ID 查询菜单 */
    fun getById(id: Long): Menu?

    /** 保存菜单 */
    fun save(menu: Menu): Menu

    /** 更新菜单 */
    fun update(menu: Menu): Menu

    /** 按 ID 删除菜单 */
    fun removeById(id: Long)
}
