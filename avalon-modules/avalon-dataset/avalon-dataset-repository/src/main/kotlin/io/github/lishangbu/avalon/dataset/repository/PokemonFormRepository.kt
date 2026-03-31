package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
interface PokemonFormRepository : KRepository<PokemonForm, Long> {
    fun listAll(): List<PokemonForm> =
        sql
            .createQuery(PokemonForm::class) {
                select(table)
            }.execute()

    fun pageViews(
        specification: PokemonFormSpecification?,
        pageable: Pageable,
    ): Page<PokemonFormView> =
        sql
            .createQuery(PokemonForm::class) {
                specification?.let(::where)
                select(table.fetch(PokemonFormView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    fun loadViewById(id: Long): PokemonFormView? =
        sql
            .createQuery(PokemonForm::class) {
                where(table.id eq id)
                select(table.fetch(PokemonFormView::class))
            }.execute()
            .firstOrNull()
}
