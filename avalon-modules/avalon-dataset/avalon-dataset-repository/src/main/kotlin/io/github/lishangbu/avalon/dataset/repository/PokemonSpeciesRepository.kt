package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * 宝可梦种类仓储接口
 *
 * 定义宝可梦种类数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/12
 */
@Repository
interface PokemonSpeciesRepository : KRepository<PokemonSpecies, Long> {
    /** 按条件查询宝可梦种族视图 */
    fun listViews(specification: PokemonSpeciesSpecification?): List<PokemonSpeciesView> =
        sql
            .createQuery(PokemonSpecies::class) {
                specification?.let(::where)
                select(table.fetch(PokemonSpeciesView::class))
            }.execute()

    /** 按条件分页查询宝可梦种族视图 */
    fun pageViews(
        specification: PokemonSpeciesSpecification?,
        pageable: Pageable,
    ): Page<PokemonSpeciesView> =
        sql
            .createQuery(PokemonSpecies::class) {
                specification?.let(::where)
                select(table.fetch(PokemonSpeciesView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询单个宝可梦种族视图 */
    fun loadViewById(id: Long): PokemonSpeciesView? =
        sql
            .createQuery(PokemonSpecies::class) {
                where(table.id eq id)
                select(table.fetch(PokemonSpeciesView::class))
            }.execute()
            .firstOrNull()
}
