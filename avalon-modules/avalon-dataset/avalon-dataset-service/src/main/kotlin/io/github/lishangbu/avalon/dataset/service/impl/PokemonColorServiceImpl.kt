package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonColorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonColorInput
import io.github.lishangbu.avalon.dataset.repository.PokemonColorRepository
import io.github.lishangbu.avalon.dataset.service.PokemonColorService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 宝可梦颜色服务实现 */
@Service
class PokemonColorServiceImpl(
    private val pokemonColorRepository: PokemonColorRepository,
) : PokemonColorService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SavePokemonColorInput): PokemonColorView = PokemonColorView(pokemonColorRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdatePokemonColorInput): PokemonColorView = PokemonColorView(pokemonColorRepository.save(command.toEntity(), SaveMode.UPSERT))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        pokemonColorRepository.deleteById(id)
    }

    override fun listByCondition(specification: PokemonColorSpecification): List<PokemonColorView> = pokemonColorRepository.listViews(specification)
}
