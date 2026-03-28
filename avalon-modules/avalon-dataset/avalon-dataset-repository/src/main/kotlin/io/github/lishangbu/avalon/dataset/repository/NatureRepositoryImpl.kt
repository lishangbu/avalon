package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Nature
import io.github.lishangbu.avalon.dataset.entity.dto.NatureSpecification
import io.github.lishangbu.avalon.dataset.entity.id
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
class NatureRepositoryImpl(
    private val sql: KSqlClient,
) : NatureRepositoryExt {
    override fun findAll(specification: NatureSpecification?): List<Nature> =
        sql
            .createQuery(Nature::class) {
                specification?.let { where(it) }
                select(table.fetch(DatasetFetchers.NATURE_WITH_ASSOCIATIONS))
            }.execute()

    override fun findByIdWithAssociations(id: Long): Nature? =
        sql
            .createQuery(Nature::class) {
                where(table.id eq id)
                select(table.fetch(DatasetFetchers.NATURE_WITH_ASSOCIATIONS))
            }.execute()
            .firstOrNull()

    override fun removeById(id: Long) {
        sql
            .createDelete(Nature::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
