package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
interface PokemonEvolutionRepository : KRepository<PokemonEvolution, Long> {
    fun pageViews(
        specification: PokemonEvolutionSpecification?,
        pageable: Pageable,
    ): Page<PokemonEvolutionView> =
        sql
            .createQuery(PokemonEvolution::class) {
                specification?.let(::where)
                select(table.fetch(PokemonEvolutionView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    fun loadViewById(id: Long): PokemonEvolutionView? =
        sql
            .createQuery(PokemonEvolution::class) {
                where(table.id eq id)
                select(table.fetch(PokemonEvolutionView::class))
            }.execute()
            .firstOrNull()

    fun listByEvolutionChainId(evolutionChainId: Long): List<PokemonEvolution> =
        sql
            .createQuery(PokemonEvolution::class) {
                where(table.evolutionChain.id eq evolutionChainId)
                select(table)
            }.execute()
            .sortedBy { it.id }

    fun countByEvolutionChainIdAndLocationId(
        evolutionChainId: Long,
        locationId: Long,
    ): Int =
        sql
            .createQuery(PokemonEvolution::class) {
                where(table.evolutionChain.id eq evolutionChainId)
                where(table.location.id eq locationId)
                select(table.id)
            }.execute()
            .size

    fun countByEvolutionChainIdAndItemId(
        evolutionChainId: Long,
        itemId: Long,
    ): Int =
        sql
            .createQuery(PokemonEvolution::class) {
                where(table.evolutionChain.id eq evolutionChainId)
                where(table.item.id eq itemId)
                select(table.id)
            }.execute()
            .size
}
