package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.Menu
import org.springframework.data.domain.Example
import org.springframework.data.domain.Sort

/**
 * 菜单仓储接口
 *
 * 定义菜单数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/08/20
 */
interface MenuRepository {
    /** 按条件查询菜单列表 */
    fun findAll(example: Example<Menu>?): List<Menu>

    /** 按条件排序查询菜单列表 */
    fun findAll(
        example: Example<Menu>?,
        sort: Sort,
    ): List<Menu>

    /** 按 ID 查询菜单 */
    fun findById(id: Long): Menu?

    /** 按 ID 列表查询菜单 */
    fun findAllById(ids: Iterable<Long>): List<Menu>

    /** 保存菜单 */
    fun save(menu: Menu): Menu

    /** 保存菜单并立即刷新 */
    fun saveAndFlush(menu: Menu): Menu

    /** 按 ID 删除菜单 */
    fun deleteById(id: Long)

    /** 刷新持久化上下文 */
    fun flush()

    /** 按排序顺序和 ID 升序查询菜单列表 */
    fun findAllByOrderBySortingOrderAscIdAsc(): List<Menu>

    /** 按角色编码列表查询菜单 */
    fun findAllByRoleCodes(roleCodes: List<String>): List<Menu>
}
