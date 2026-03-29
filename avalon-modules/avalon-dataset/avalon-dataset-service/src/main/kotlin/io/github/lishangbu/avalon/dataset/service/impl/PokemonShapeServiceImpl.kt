package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonShapeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonShapeInput
import io.github.lishangbu.avalon.dataset.repository.PokemonShapeRepository
import io.github.lishangbu.avalon.dataset.service.PokemonShapeService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 宝可梦形状服务实现 */
@Service
class PokemonShapeServiceImpl(
    private val pokemonShapeRepository: PokemonShapeRepository,
) : PokemonShapeService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SavePokemonShapeInput): PokemonShapeView = PokemonShapeView(pokemonShapeRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdatePokemonShapeInput): PokemonShapeView = PokemonShapeView(pokemonShapeRepository.save(command.toEntity(), SaveMode.UPSERT))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        pokemonShapeRepository.deleteById(id)
    }

    override fun listByCondition(specification: PokemonShapeSpecification): List<PokemonShapeView> = pokemonShapeRepository.listViews(specification)
}
