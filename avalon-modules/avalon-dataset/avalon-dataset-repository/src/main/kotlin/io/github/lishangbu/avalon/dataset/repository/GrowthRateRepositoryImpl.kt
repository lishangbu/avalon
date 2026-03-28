package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import io.github.lishangbu.avalon.dataset.entity.id
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
class GrowthRateRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : GrowthRateRepositoryExt {
    /** 按条件查询成长速率列表 */
    override fun findAll(specification: GrowthRateSpecification?): List<GrowthRate> =
        sql
            .createQuery(GrowthRate::class) {
                specification?.let { where(it) }
                select(table)
            }.execute()

    /** 按 ID 删除成长速率 */
    override fun removeById(id: Long) {
        sql
            .createDelete(GrowthRate::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
