package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonHabitatInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonHabitatInput

/** 宝可梦栖息地服务 */
interface PokemonHabitatService {
    /** 创建宝可梦栖息地 */
    fun save(command: SavePokemonHabitatInput): PokemonHabitatView

    /** 更新宝可梦栖息地 */
    fun update(command: UpdatePokemonHabitatInput): PokemonHabitatView

    /** 删除指定 ID 的宝可梦栖息地 */
    fun removeById(id: Long)

    /** 按筛选条件查询宝可梦栖息地列表 */
    fun listByCondition(specification: PokemonHabitatSpecification): List<PokemonHabitatView>
}
