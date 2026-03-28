package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.entity.id
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
class TypeRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : TypeRepositoryExt {
    /** 按条件查询属性列表 */
    override fun findAll(specification: TypeSpecification?): List<Type> =
        sql
            .createQuery(Type::class) {
                specification?.let { where(it) }
                select(table)
            }.execute()

    /** 按 ID 删除属性 */
    override fun removeById(id: Long) {
        sql
            .createDelete(Type::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
