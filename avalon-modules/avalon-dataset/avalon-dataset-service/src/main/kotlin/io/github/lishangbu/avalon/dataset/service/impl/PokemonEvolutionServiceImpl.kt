package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.PokemonEvolution
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonEvolutionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonEvolutionInput
import io.github.lishangbu.avalon.dataset.repository.PokemonEvolutionRepository
import io.github.lishangbu.avalon.dataset.service.PokemonEvolutionService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class PokemonEvolutionServiceImpl(
    private val pokemonEvolutionRepository: PokemonEvolutionRepository,
) : PokemonEvolutionService {
    override fun getPageByCondition(
        specification: PokemonEvolutionSpecification,
        pageable: Pageable,
    ): Page<PokemonEvolutionView> = pokemonEvolutionRepository.pageViews(specification, pageable)

    override fun save(command: SavePokemonEvolutionInput): PokemonEvolutionView = pokemonEvolutionRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    override fun update(command: UpdatePokemonEvolutionInput): PokemonEvolutionView = pokemonEvolutionRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    override fun removeById(id: Long) {
        pokemonEvolutionRepository.deleteById(id)
    }

    private fun reloadView(pokemonEvolution: PokemonEvolution): PokemonEvolutionView = requireNotNull(pokemonEvolutionRepository.loadViewById(pokemonEvolution.id)) { "未找到 ID=${pokemonEvolution.id} 对应的进化条件" }
}
