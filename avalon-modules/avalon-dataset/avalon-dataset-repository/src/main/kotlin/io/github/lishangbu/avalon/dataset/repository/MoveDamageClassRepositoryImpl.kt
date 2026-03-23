package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class MoveDamageClassRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : MoveDamageClassRepository {
    /** 按条件查询招式伤害分类列表 */
    override fun findAll(example: Example<MoveDamageClass>?): List<MoveDamageClass> {
        val probe = example?.probe
        return sql
            .createQuery(MoveDamageClass::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                probe.readOrNull { description }.takeFilter()?.let { where(table.description ilike "%$it%") }
                select(table)
            }.execute()
    }

    /** 按条件分页查询招式伤害分类 */
    override fun findAll(
        example: Example<MoveDamageClass>?,
        pageable: Pageable,
    ): Page<MoveDamageClass> {
        val probe = example?.probe
        return sql
            .createQuery(MoveDamageClass::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                probe.readOrNull { description }.takeFilter()?.let { where(table.description ilike "%$it%") }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
    }

    /** 保存招式伤害分类 */
    override fun save(moveDamageClass: MoveDamageClass): MoveDamageClass = sql.save(moveDamageClass).modifiedEntity

    /** 按 ID 删除招式伤害分类 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(MoveDamageClass::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
