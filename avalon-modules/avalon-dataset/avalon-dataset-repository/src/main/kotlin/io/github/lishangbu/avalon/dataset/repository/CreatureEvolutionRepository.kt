package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
interface CreatureEvolutionRepository : KRepository<CreatureEvolution, Long> {
    fun pageViews(
        specification: CreatureEvolutionSpecification?,
        pageable: Pageable,
    ): Page<CreatureEvolutionView> =
        sql
            .createQuery(CreatureEvolution::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureEvolutionView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    fun loadViewById(id: Long): CreatureEvolutionView? =
        sql
            .createQuery(CreatureEvolution::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureEvolutionView::class))
            }.execute()
            .firstOrNull()

    fun listByEvolutionChainId(evolutionChainId: Long): List<CreatureEvolution> =
        sql
            .createQuery(CreatureEvolution::class) {
                where(table.evolutionChain.id eq evolutionChainId)
                orderBy(DEFAULT_SORT)
                select(table)
            }.execute()
            .sortedBy { it.id }

    fun countByEvolutionChainIdAndLocationId(
        evolutionChainId: Long,
        locationId: Long,
    ): Int =
        sql
            .createQuery(CreatureEvolution::class) {
                where(table.evolutionChain.id eq evolutionChainId)
                where(table.location.id eq locationId)
                orderBy(DEFAULT_SORT)
                select(table.id)
            }.execute()
            .size

    fun countByEvolutionChainIdAndItemId(
        evolutionChainId: Long,
        itemId: Long,
    ): Int =
        sql
            .createQuery(CreatureEvolution::class) {
                where(table.evolutionChain.id eq evolutionChainId)
                where(table.item.id eq itemId)
                orderBy(DEFAULT_SORT)
                select(table.id)
            }.execute()
            .size

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
