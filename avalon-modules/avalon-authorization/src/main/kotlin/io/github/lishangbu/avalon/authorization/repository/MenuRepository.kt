package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.Menu
import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.data.domain.Sort

/**
 * 菜单仓储接口
 *
 * 定义菜单数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/08/20
 */
interface MenuRepository :
    KRepository<Menu, Long>,
    MenuRepositoryExt

interface MenuRepositoryExt {
    /** 按条件查询菜单列表 */
    fun findAll(specification: MenuSpecification?): List<Menu>

    /** 按条件排序查询菜单列表 */
    fun findAll(
        specification: MenuSpecification?,
        sort: Sort,
    ): List<Menu>

    /** 按 ID 删除菜单 */
    fun removeById(id: Long)

    /** 按排序顺序和 ID 升序查询菜单列表 */
    fun findAllByOrderBySortingOrderAscIdAsc(): List<Menu>

    /** 按角色编码列表查询菜单 */
    fun findAllByRoleCodes(roleCodes: List<String>): List<Menu>
}
