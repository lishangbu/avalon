package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.PokemonSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonSpeciesInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonSpeciesInput
import io.github.lishangbu.avalon.dataset.repository.PokemonSpeciesRepository
import io.github.lishangbu.avalon.dataset.service.PokemonSpeciesService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 宝可梦种族应用服务实现 */
@Service
class PokemonSpeciesServiceImpl(
    private val pokemonSpeciesRepository: PokemonSpeciesRepository,
) : PokemonSpeciesService {
    /** 按筛选条件分页查询宝可梦种族 */
    override fun getPageByCondition(
        specification: PokemonSpeciesSpecification,
        pageable: Pageable,
    ): Page<PokemonSpeciesView> = pokemonSpeciesRepository.pageViews(specification, pageable)

    /** 创建宝可梦种族 */
    override fun save(command: SavePokemonSpeciesInput): PokemonSpeciesView = pokemonSpeciesRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新宝可梦种族 */
    override fun update(command: UpdatePokemonSpeciesInput): PokemonSpeciesView = pokemonSpeciesRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    /** 删除指定 ID 的宝可梦种族 */
    override fun removeById(id: Long) {
        pokemonSpeciesRepository.deleteById(id)
    }

    private fun reloadView(pokemonSpecies: PokemonSpecies): PokemonSpeciesView = requireNotNull(pokemonSpeciesRepository.loadViewById(pokemonSpecies.id)) { "未找到 ID=${pokemonSpecies.id} 对应的宝可梦种族" }
}
