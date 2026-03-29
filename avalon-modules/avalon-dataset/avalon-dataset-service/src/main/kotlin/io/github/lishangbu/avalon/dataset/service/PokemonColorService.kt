package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonColorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonColorInput

/** 宝可梦颜色服务 */
interface PokemonColorService {
    /** 创建宝可梦颜色 */
    fun save(command: SavePokemonColorInput): PokemonColorView

    /** 更新宝可梦颜色 */
    fun update(command: UpdatePokemonColorInput): PokemonColorView

    /** 删除指定 ID 的宝可梦颜色 */
    fun removeById(id: Long)

    /** 按筛选条件查询宝可梦颜色列表 */
    fun listByCondition(specification: PokemonColorSpecification): List<PokemonColorView>
}
