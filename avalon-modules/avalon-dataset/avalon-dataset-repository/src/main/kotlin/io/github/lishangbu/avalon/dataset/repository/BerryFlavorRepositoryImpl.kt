package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.entity.id
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
class BerryFlavorRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : BerryFlavorRepositoryExt {
    /** 按条件查询树果风味列表 */
    override fun findAll(specification: BerryFlavorSpecification?): List<BerryFlavor> =
        sql
            .createQuery(BerryFlavor::class) {
                specification?.let { where(it) }
                select(table)
            }.execute()

    /** 按 ID 删除树果风味 */
    override fun removeById(id: Long) {
        sql
            .createDelete(BerryFlavor::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
