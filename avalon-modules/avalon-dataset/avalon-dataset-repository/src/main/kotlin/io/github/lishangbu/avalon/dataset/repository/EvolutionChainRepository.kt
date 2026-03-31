package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * 进化链仓储接口
 *
 * 定义进化链数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/12
 */
@Repository
interface EvolutionChainRepository : KRepository<EvolutionChain, Long> {
    fun listViews(specification: EvolutionChainSpecification?): List<EvolutionChainView> =
        sql
            .createQuery(EvolutionChain::class) {
                specification?.let(::where)
                select(table.fetch(EvolutionChainView::class))
            }.execute()

    fun pageViews(
        specification: EvolutionChainSpecification?,
        pageable: Pageable,
    ): Page<EvolutionChainView> =
        sql
            .createQuery(EvolutionChain::class) {
                specification?.let(::where)
                select(table.fetch(EvolutionChainView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    fun loadViewById(id: Long): EvolutionChainView? =
        sql
            .createQuery(EvolutionChain::class) {
                where(table.id eq id)
                select(table.fetch(EvolutionChainView::class))
            }.execute()
            .firstOrNull()

    fun loadBabyTriggerItemId(id: Long): Long? =
        sql
            .createQuery(EvolutionChain::class) {
                where(table.id eq id)
                select(table.babyTriggerItem.id)
            }.execute()
            .firstOrNull()
}
