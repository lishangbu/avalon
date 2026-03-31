package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 宝可梦应用服务 */
interface PokemonService {
    /** 按筛选条件分页查询宝可梦 */
    fun getPageByCondition(
        specification: PokemonSpecification,
        pageable: Pageable,
    ): Page<PokemonView>

    /** 创建宝可梦 */
    fun save(command: SavePokemonInput): PokemonView

    /** 更新宝可梦 */
    fun update(command: UpdatePokemonInput): PokemonView

    /** 删除指定 ID 的宝可梦 */
    fun removeById(id: Long)
}
