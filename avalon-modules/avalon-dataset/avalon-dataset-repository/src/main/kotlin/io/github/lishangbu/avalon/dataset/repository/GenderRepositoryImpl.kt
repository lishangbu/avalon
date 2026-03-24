package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.entity.id
import io.github.lishangbu.avalon.dataset.entity.internalName
import io.github.lishangbu.avalon.dataset.entity.name
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.data.domain.Example
import org.springframework.stereotype.Repository

@Repository
class GenderRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : GenderRepository {
    /** 查询全部性别列表 */
    override fun findAll(): List<Gender> =
        sql
            .createQuery(Gender::class) {
                select(table)
            }.execute()

    /** 按条件查询性别列表 */
    override fun findAll(example: Example<Gender>?): List<Gender> {
        val probe = example?.probe
        return sql
            .createQuery(Gender::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                select(table)
            }.execute()
    }

    /** 按 ID 查询性别 */
    override fun findById(id: Long): Gender? = sql.findById(Gender::class, id)

    /** 保存性别 */
    override fun save(gender: Gender): Gender =
        sql
            .save(gender) {
                val mode = gender.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    /** 保存性别并立即刷新 */
    override fun saveAndFlush(gender: Gender): Gender = save(gender)

    /** 按 ID 删除性别 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(Gender::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 刷新持久化上下文 */
    override fun flush() = Unit
}
