package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.PokemonForm
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonFormInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonFormInput
import io.github.lishangbu.avalon.dataset.repository.PokemonFormRepository
import io.github.lishangbu.avalon.dataset.service.PokemonFormService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class PokemonFormServiceImpl(
    private val pokemonFormRepository: PokemonFormRepository,
) : PokemonFormService {
    override fun getPageByCondition(
        specification: PokemonFormSpecification,
        pageable: Pageable,
    ): Page<PokemonFormView> = pokemonFormRepository.pageViews(specification, pageable)

    override fun save(command: SavePokemonFormInput): PokemonFormView = pokemonFormRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    override fun update(command: UpdatePokemonFormInput): PokemonFormView = pokemonFormRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    override fun removeById(id: Long) {
        pokemonFormRepository.deleteById(id)
    }

    private fun reloadView(pokemonForm: PokemonForm): PokemonFormView = requireNotNull(pokemonFormRepository.loadViewById(pokemonForm.id)) { "未找到 ID=${pokemonForm.id} 对应的宝可梦形态" }
}
