package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Pokemon
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonInput
import io.github.lishangbu.avalon.dataset.repository.PokemonRepository
import io.github.lishangbu.avalon.dataset.service.PokemonService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 宝可梦应用服务实现 */
@Service
class PokemonServiceImpl(
    private val pokemonRepository: PokemonRepository,
) : PokemonService {
    /** 按筛选条件分页查询宝可梦 */
    override fun getPageByCondition(
        specification: PokemonSpecification,
        pageable: Pageable,
    ): Page<PokemonView> = pokemonRepository.pageViews(specification, pageable)

    /** 创建宝可梦 */
    override fun save(command: SavePokemonInput): PokemonView = pokemonRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新宝可梦 */
    override fun update(command: UpdatePokemonInput): PokemonView = pokemonRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    /** 删除指定 ID 的宝可梦 */
    override fun removeById(id: Long) {
        pokemonRepository.deleteById(id)
    }

    private fun reloadView(pokemon: Pokemon): PokemonView = requireNotNull(pokemonRepository.loadViewById(pokemon.id)) { "未找到 ID=${pokemon.id} 对应的宝可梦" }
}
