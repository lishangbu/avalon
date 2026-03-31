package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonSpeciesInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonSpeciesInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 宝可梦种族应用服务 */
interface PokemonSpeciesService {
    /** 按筛选条件分页查询宝可梦种族 */
    fun getPageByCondition(
        specification: PokemonSpeciesSpecification,
        pageable: Pageable,
    ): Page<PokemonSpeciesView>

    /** 创建宝可梦种族 */
    fun save(command: SavePokemonSpeciesInput): PokemonSpeciesView

    /** 更新宝可梦种族 */
    fun update(command: UpdatePokemonSpeciesInput): PokemonSpeciesView

    /** 删除指定 ID 的宝可梦种族 */
    fun removeById(id: Long)
}
