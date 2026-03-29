package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

/** 宝可梦栖息地仓储接口 */
@Repository
interface PokemonHabitatRepository : KRepository<PokemonHabitat, Long> {
    /** 按条件查询宝可梦栖息地视图 */
    fun listViews(specification: PokemonHabitatSpecification?): List<PokemonHabitatView> =
        sql
            .createQuery(PokemonHabitat::class) {
                specification?.let(::where)
                select(table.fetch(PokemonHabitatView::class))
            }.execute()

    /** 按 ID 查询宝可梦栖息地视图 */
    fun loadViewById(id: Long): PokemonHabitatView? =
        sql
            .createQuery(PokemonHabitat::class) {
                where(table.id eq id)
                select(table.fetch(PokemonHabitatView::class))
            }.execute()
            .firstOrNull()
}
