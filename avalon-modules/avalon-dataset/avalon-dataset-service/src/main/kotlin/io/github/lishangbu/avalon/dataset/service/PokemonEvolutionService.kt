package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonEvolutionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonEvolutionInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

interface PokemonEvolutionService {
    fun getPageByCondition(
        specification: PokemonEvolutionSpecification,
        pageable: Pageable,
    ): Page<PokemonEvolutionView>

    fun save(command: SavePokemonEvolutionInput): PokemonEvolutionView

    fun update(command: UpdatePokemonEvolutionInput): PokemonEvolutionView

    fun removeById(id: Long)
}
