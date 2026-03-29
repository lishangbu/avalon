package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

/** 宝可梦形状仓储接口 */
@Repository
interface PokemonShapeRepository : KRepository<PokemonShape, Long> {
    /** 按条件查询宝可梦形状视图 */
    fun listViews(specification: PokemonShapeSpecification?): List<PokemonShapeView> =
        sql
            .createQuery(PokemonShape::class) {
                specification?.let(::where)
                select(table.fetch(PokemonShapeView::class))
            }.execute()

    /** 按 ID 查询宝可梦形状视图 */
    fun loadViewById(id: Long): PokemonShapeView? =
        sql
            .createQuery(PokemonShape::class) {
                where(table.id eq id)
                select(table.fetch(PokemonShapeView::class))
            }.execute()
            .firstOrNull()
}
