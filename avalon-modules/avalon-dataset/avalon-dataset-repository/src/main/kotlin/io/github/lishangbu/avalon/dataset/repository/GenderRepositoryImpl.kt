package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import io.github.lishangbu.avalon.dataset.entity.id
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
class GenderRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : GenderRepositoryExt {
    /** 按条件查询性别列表 */
    override fun findAll(specification: GenderSpecification?): List<Gender> =
        sql
            .createQuery(Gender::class) {
                specification?.let { where(it) }
                select(table)
            }.execute()

    /** 按 ID 删除性别 */
    override fun removeById(id: Long) {
        sql
            .createDelete(Gender::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
