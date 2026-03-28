package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class MenuRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : MenuRepository {
    /** 按条件查询菜单列表 */
    override fun findAll(specification: MenuSpecification?): List<Menu> =
        sql
            .createQuery(Menu::class) {
                specification?.let { where(it) }
                select(table.fetch(AuthorizationFetchers.MENU))
            }.execute()

    /** 按条件排序查询菜单列表 */
    override fun findAll(
        specification: MenuSpecification?,
        sort: Sort,
    ): List<Menu> =
        sql
            .createQuery(Menu::class) {
                specification?.let { where(it) }
                orderBy(sort)
                select(table.fetch(AuthorizationFetchers.MENU))
            }.execute()

    /** 按 ID 查询菜单 */
    override fun findById(id: Long): Menu? =
        sql
            .createQuery(Menu::class) {
                where(table.id eq id)
                select(table.fetch(AuthorizationFetchers.MENU))
            }.execute()
            .firstOrNull()

    /** 按 ID 列表查询菜单 */
    override fun findAllById(ids: Iterable<Long>): List<Menu> = ids.mapNotNull(::findById)

    /** 保存菜单 */
    override fun save(menu: Menu): Menu =
        sql
            .save(menu) {
                val mode = menu.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    /** 保存菜单并立即刷新 */
    override fun saveAndFlush(menu: Menu): Menu = save(menu)

    /** 按 ID 删除菜单 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(Menu::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 刷新持久化上下文 */
    override fun flush() = Unit

    /** 按排序顺序和 ID 升序查询菜单列表 */
    override fun findAllByOrderBySortingOrderAscIdAsc(): List<Menu> =
        sql
            .createQuery(Menu::class) {
                orderBy(Sort.by(Sort.Order.asc("sortingOrder"), Sort.Order.asc("id")))
                select(table.fetch(AuthorizationFetchers.MENU))
            }.execute()

    /** 按角色编码列表查询菜单 */
    override fun findAllByRoleCodes(roleCodes: List<String>): List<Menu> {
        if (roleCodes.isEmpty()) {
            return emptyList()
        }
        val menus =
            roleCodes.flatMap { roleCode ->
                sql
                    .createQuery(Role::class) {
                        where(table.code eq roleCode)
                        select(table.fetch(AuthorizationFetchers.ROLE_WITH_MENUS))
                    }.execute()
                    .flatMap { it.menus }
            }
        return menus
            .distinctBy { it.id }
            .sortedWith(
                compareBy<Menu> { it.sortingOrder ?: Int.MAX_VALUE }
                    .thenBy { it.id },
            )
    }
}
