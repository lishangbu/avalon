package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EggGroup
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupSpecification
import io.github.lishangbu.avalon.dataset.entity.id
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
class EggGroupRepositoryImpl(
    private val sql: KSqlClient,
) : EggGroupRepositoryExt {
    override fun findAll(specification: EggGroupSpecification?): List<EggGroup> =
        sql
            .createQuery(EggGroup::class) {
                specification?.let { where(it) }
                select(table)
            }.execute()

    override fun removeById(id: Long) {
        sql
            .createDelete(EggGroup::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
