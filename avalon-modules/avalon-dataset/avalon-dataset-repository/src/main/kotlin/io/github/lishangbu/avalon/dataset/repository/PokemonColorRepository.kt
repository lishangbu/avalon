package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

/** 宝可梦颜色仓储接口 */
@Repository
interface PokemonColorRepository : KRepository<PokemonColor, Long> {
    /** 按条件查询宝可梦颜色视图 */
    fun listViews(specification: PokemonColorSpecification?): List<PokemonColorView> =
        sql
            .createQuery(PokemonColor::class) {
                specification?.let(::where)
                select(table.fetch(PokemonColorView::class))
            }.execute()

    /** 按 ID 查询宝可梦颜色视图 */
    fun loadViewById(id: Long): PokemonColorView? =
        sql
            .createQuery(PokemonColor::class) {
                where(table.id eq id)
                select(table.fetch(PokemonColorView::class))
            }.execute()
            .firstOrNull()
}
