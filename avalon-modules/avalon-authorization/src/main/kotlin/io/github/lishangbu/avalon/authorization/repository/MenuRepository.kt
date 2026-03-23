package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.Menu
import org.springframework.data.domain.Example
import org.springframework.data.domain.Sort

/**
 * 菜单表数据存储
 *
 * 提供基础的 CRUD 操作
 *
 * @author lishangbu
 * @since 2025/08/20
 */
interface MenuRepository {
    fun findAll(example: Example<Menu>?): List<Menu>

    fun findAll(
        example: Example<Menu>?,
        sort: Sort,
    ): List<Menu>

    fun findById(id: Long): Menu?

    fun findAllById(ids: Iterable<Long>): List<Menu>

    fun save(menu: Menu): Menu

    fun saveAndFlush(menu: Menu): Menu

    fun deleteById(id: Long)

    fun flush()

    fun findAllByOrderBySortingOrderAscIdAsc(): List<Menu>

    fun findAllByRoleCodes(roleCodes: List<String>): List<Menu>
}
