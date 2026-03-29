package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonHabitatInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonHabitatInput
import io.github.lishangbu.avalon.dataset.repository.PokemonHabitatRepository
import io.github.lishangbu.avalon.dataset.service.PokemonHabitatService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 宝可梦栖息地服务实现 */
@Service
class PokemonHabitatServiceImpl(
    private val pokemonHabitatRepository: PokemonHabitatRepository,
) : PokemonHabitatService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SavePokemonHabitatInput): PokemonHabitatView = PokemonHabitatView(pokemonHabitatRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdatePokemonHabitatInput): PokemonHabitatView = PokemonHabitatView(pokemonHabitatRepository.save(command.toEntity(), SaveMode.UPSERT))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        pokemonHabitatRepository.deleteById(id)
    }

    override fun listByCondition(specification: PokemonHabitatSpecification): List<PokemonHabitatView> = pokemonHabitatRepository.listViews(specification)
}
