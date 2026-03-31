package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * 宝可梦仓储接口
 *
 * 定义宝可梦数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface PokemonRepository : KRepository<Pokemon, Long> {
    fun listAll(): List<Pokemon> =
        sql
            .createQuery(Pokemon::class) {
                select(table)
            }.execute()

    /** 按条件查询宝可梦视图 */
    fun listViews(specification: PokemonSpecification?): List<PokemonView> =
        sql
            .createQuery(Pokemon::class) {
                specification?.let(::where)
                select(table.fetch(PokemonView::class))
            }.execute()

    /** 按条件分页查询宝可梦视图 */
    fun pageViews(
        specification: PokemonSpecification?,
        pageable: Pageable,
    ): Page<PokemonView> =
        sql
            .createQuery(Pokemon::class) {
                specification?.let(::where)
                select(table.fetch(PokemonView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询单个宝可梦视图 */
    fun loadViewById(id: Long): PokemonView? =
        sql
            .createQuery(Pokemon::class) {
                where(table.id eq id)
                select(table.fetch(PokemonView::class))
            }.execute()
            .firstOrNull()
}
