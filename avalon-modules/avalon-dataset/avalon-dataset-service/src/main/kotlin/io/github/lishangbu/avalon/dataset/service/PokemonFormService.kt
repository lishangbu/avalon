package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonFormInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonFormInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

interface PokemonFormService {
    fun getPageByCondition(
        specification: PokemonFormSpecification,
        pageable: Pageable,
    ): Page<PokemonFormView>

    fun save(command: SavePokemonFormInput): PokemonFormView

    fun update(command: UpdatePokemonFormInput): PokemonFormView

    fun removeById(id: Long)
}
