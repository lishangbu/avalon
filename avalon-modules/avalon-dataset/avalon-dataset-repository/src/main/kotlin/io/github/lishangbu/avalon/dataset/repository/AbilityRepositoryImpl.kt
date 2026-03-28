package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Ability
import io.github.lishangbu.avalon.dataset.entity.dto.AbilitySpecification
import io.github.lishangbu.avalon.dataset.entity.id
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
class AbilityRepositoryImpl(
    private val sql: KSqlClient,
) : AbilityRepositoryExt {
    override fun findAll(specification: AbilitySpecification?): List<Ability> =
        sql
            .createQuery(Ability::class) {
                specification?.let { where(it) }
                select(table)
            }.execute()

    override fun removeById(id: Long) {
        sql
            .createDelete(Ability::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
