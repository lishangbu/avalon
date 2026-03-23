package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class RoleRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : RoleRepository {
    /** 按条件查询角色列表 */
    override fun findAll(example: Example<Role>?): List<Role> {
        val probe = example?.probe
        return sql
            .createQuery(Role::class) {
                probe.readId()?.let { where(table.id eq it) }
                probe.readCode().takeFilter()?.let { where(table.code ilike "%$it%") }
                probe.readName().takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readEnabled()?.let { where(table.enabled eq it) }
                select(table.fetch(ROLE_WITH_MENUS_FETCHER))
            }.execute()
    }

    /** 按条件分页查询角色 */
    override fun findAll(
        example: Example<Role>?,
        pageable: Pageable,
    ): Page<Role> {
        val probe = example?.probe
        return sql
            .createQuery(Role::class) {
                probe.readId()?.let { where(table.id eq it) }
                probe.readCode().takeFilter()?.let { where(table.code ilike "%$it%") }
                probe.readName().takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readEnabled()?.let { where(table.enabled eq it) }
                select(table.fetch(ROLE_WITH_MENUS_FETCHER))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
    }

    /** 按 ID 查询角色 */
    override fun findById(id: Long): Role? =
        sql
            .createQuery(Role::class) {
                where(table.id eq id)
                select(table.fetch(ROLE_WITH_MENUS_FETCHER))
            }.execute()
            .firstOrNull()

    /** 按 ID 列表查询角色 */
    override fun findAllById(ids: Iterable<Long>): List<Role> = ids.mapNotNull { sql.findById(Role::class, it) }

    /** 保存角色 */
    override fun save(role: Role): Role =
        sql
            .save(role) {
                val mode = role.readId()?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    /** 保存角色并立即刷新 */
    override fun saveAndFlush(role: Role): Role = save(role)

    /** 按 ID 删除角色 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(Role::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 刷新持久化上下文 */
    override fun flush() = Unit

    companion object {
        /** 角色及菜单列表抓取器 */
        private val ROLE_WITH_MENUS_FETCHER =
            newFetcher(Role::class).`by` {
                allScalarFields()
                menus {
                    allScalarFields()
                }
            }
    }

    /** 安全读取主键 */
    private fun Role?.readId(): Long? = readOrNull { id }

    /** 读取状态码 */
    private fun Role?.readCode(): String? = readOrNull { code }

    /** 读取名称 */
    private fun Role?.readName(): String? = readOrNull { name }

    /** 读取启用状态 */
    private fun Role?.readEnabled(): Boolean? = readOrNull { enabled }
}
